package de.serdioa.common.pool;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.PhantomReference;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


// Simple implementation using concurrent map.
public class ConcurrentSharedObjectPool<K, S extends SharedObject, P extends PooledObject> implements
        SharedObjectPool<K, S> {

    private static final Logger logger = LoggerFactory.getLogger(ConcurrentSharedObjectPool.class);

    // Factory for creating new pooled objects.
    private PooledObjectFactory<K, P> pooledObjectFactory;

    // Factory for creating shared objects from pooled objects.
    private SharedObjectFactory<P, S> sharedObjectFactory;

    // Pooled entries.
    private final ConcurrentMap<K, Entry> entries = new ConcurrentHashMap<>();

    // Should we actually dispose unused entries?
    private boolean disposeUnusedEntries = true;

    // A queue with phantom references on shared objects. We keep them to be able to find shared objects which were not
    // properly disposed of.
    private final ReferenceQueue<S> sharedObjectsRefQueue = new ReferenceQueue<>();

    // The thread processing phantom references on shared objects claimed by the GC.
    private Thread sharedObjectsRipper;

    // The synchronization lock for lifecycle events (startup / shutdown).
    private final Object lifecycleMonitor = new Object();


    public ConcurrentSharedObjectPool() {
        synchronized (this.lifecycleMonitor) {
            this.sharedObjectsRipper = new Thread(this::reapSharedObjects, this.getClass().getName() + "-ripper");
            this.sharedObjectsRipper.setDaemon(true);
            this.sharedObjectsRipper.start();
        }
    }


    public void setPooledObjectFactory(PooledObjectFactory<K, P> factory) {
        this.pooledObjectFactory = Objects.requireNonNull(factory);
    }


    public void setSharedObjectFactory(SharedObjectFactory<P, S> factory) {
        this.sharedObjectFactory = Objects.requireNonNull(factory);
    }


    public void setDisposeUnusedEntries(boolean disposeUnusedEntries) {
        this.disposeUnusedEntries = disposeUnusedEntries;
    }


    public void dispose() {
        synchronized (this.lifecycleMonitor) {
            if (this.sharedObjectsRipper != null) {
                this.sharedObjectsRipper.interrupt();
                this.sharedObjectsRipper = null;
            }
        }
    }


    private void reapSharedObjects() {
        try {
            while (true) {
                SharedObjectPhantomReference<?, ? extends S> ref
                        = (SharedObjectPhantomReference<?, ? extends S>) this.sharedObjectsRefQueue.remove();
                try {
                    ref.disposeIfRequired();
                } catch (Exception ex) {
                    logger.error("Exception when disposing of shared object {}", ref.getKey(), ex);
                }
            }
        } catch (InterruptedException ex) {
            // The reaper thread has been interrupted. Propagate the interruption status to the caller.
            Thread.currentThread().interrupt();
        }
    }


    @Override
    public S get(K key) throws InvalidKeyException, InitializationException {
        // Using a loop because several attempts may be required due to asynchronous operations in other threads.
        while (true) {
            // Get an entry, or create a new one. If a new entry is created, it is not initialized yet.
            Entry entry = this.entries.computeIfAbsent(key, this::createEntry);

            // Optimistically assume that the entry is already active, so we do not require an exclusive lock.
            // If our assumption is wrong, handle more complicated cases afterwards.
            S sharedObject = optimisticGetSharedObject(entry);
            if (sharedObject != null) {
                return sharedObject;
            }

            // Handle more complicated cases without additional assumptions, this requires an exclusive lock.
            // Note that even in this case the method may return null due to asynchronous operations in other threads,
            // requiring repeated attempts.
            sharedObject = getSharedObject(entry);
            if (sharedObject != null) {
                return sharedObject;
            }

            // Another attempt is required if the entry which we got was already disposed of. In the next attempt we
            // will create a new entry, or another thread may have done the same in a meantime.
        }
    }


    // Attempt to get a shared object from the specified entry optimistically, that is, assuming that the entry
    // is active. Returns the obtained shared object, or null if the assumption was wrong.
    private S optimisticGetSharedObject(Entry entry) {
        Lock sharedEntryLock = entry.sharedLock();
        sharedEntryLock.lock();
        try {
            if (entry.getSharedCount() >= 0) {
                // Our optimistic assumption was right: the entry is active, so we may just create a new shared object.
                return entry.createSharedObject();
            } else {
                // Our optimistic assumption was wrong: the entry is not initialized yet, or is already disposed of.
                return null;
            }
        } finally {
            sharedEntryLock.unlock();
        }
    }


    // Attempt to get a shared object from the specified entry without any assumptions.
    // Returns the obtained shared object, or null if it is not possible. In the latter case, another attempt
    // is required.
    private S getSharedObject(Entry entry) throws InitializationException {
        Lock exclusiveEntryLock = entry.exclusiveLock();
        exclusiveEntryLock.lock();
        try {
            int entrySharedCount = entry.getSharedCount();

            if (entrySharedCount >= 0) {
                // The entry is active, so we may just create a new shared object.
                return entry.createSharedObject();
            } else if (entrySharedCount == Entry.NEW) {
                // The entry was just added to the pool either by this thread or by another thread.
                // Since this thread synchronized on the entry first, we have to initialize it.
                try {
                    entry.init();
                } catch (Exception ex) {
                    // If an attempt to initialize an entry caused an exception, we will not attempt to create
                    // and initialize an entry again, because it could cause an infinite loop.
                    // Instead, we are re-throwing the exception.
                    K key = entry.getKey();
                    this.entries.remove(key, entry);
                    throw InitializationException.wrap(key, ex);
                }
                return entry.createSharedObject();
            } else {
                assert (entrySharedCount == Entry.DISPOSED);

                // The entry is already disposed of. This could happens, for example, in the following scenario:
                // * This thread (A) got an entry from the cache. Currently entry supports 1 shared object.
                // * Before this thread synchronizes on the entry, another thread (B) synchronizes on the entry
                // and disposes of the shared object.
                // * Since the entry does not support any shared object anymore, it is eligible for disposal.
                // * The entry is disposed and removed from the cache, but this thread (A) already holds the entry.
                // * Another thread (B) releases the synchronization lock on the entry.
                // * This thread (A) synchronizes on the entry, but the entry is already disposed of.
                //
                // Examples above is just one of several possible scenarios. Either way, we should expect the case
                // when the entry is already disposed of. In such case we will remove the disposed entry from the cache,
                // and try once more.
                K key = entry.getKey();
                this.entries.remove(key, entry);
                return null;
            }
        } finally {
            exclusiveEntryLock.unlock();
        }
    }


    private Entry createEntry(K key) throws InvalidKeyException {
        P pooledObject = createPooledObject(key);
        return new Entry(key, pooledObject);
    }


    protected P createPooledObject(K key) throws InvalidKeyException {
        return this.pooledObjectFactory.create(key);
    }


    protected S createSharedObject(P pooledObject, Runnable disposeCallback) {
        return this.sharedObjectFactory.createShared(pooledObject, disposeCallback);
    }


    private void offerDisposeEntry(Entry entry) {
        if (!this.disposeUnusedEntries) {
            // Fast track if disposing of unused entries is disabled.
            return;
        }

        Lock exclusiveEntryLock = entry.exclusiveLock();
        exclusiveEntryLock.lock();
        try {
            // An entry may be disposed of only if it is not providing any shared objects. Since we are holding
            // the exclusive lock, a number of provided shared objects can't change in the meantime.
            int entrySharedCount = entry.getSharedCount();
            if (entrySharedCount > 0) {
                // The entry is providing some shared objects, so it can't be disposed of.
                return;
            } else if (entrySharedCount == Entry.DISPOSED) {
                // Another thread had already disposed of the entry. This could happens in the following scenario:
                // * Another thread (B) released the last shared object from the entry, and called this method
                // to offer the entry for disposal.
                // * Before another thread (B) is able to obtain an exclusive lock, this thread (A) obtains
                // the lock, acquires a new shared object, releases the shared object, and calls this method to offer
                // the entry for disposal.
                // * Another thread (B) obtain the exclusive lock and disposes of the entry.
                // * This thread (A) obtains the exclusive lock, but the entry is already disposed of.
                return;
            } else if (entrySharedCount == Entry.NEW) {
                throw new IllegalStateException("Entry offered for disposal, but the status is new: " + entry.getKey());
            }

            // Dispose of the entry. Note that the entry still remains in the cache. The entry will be removed
            // from the cache after the lock is released. It makes no sense to remove the entry inside the locked
            // section, because the cache with entries is a ConcurrentMap which may be accessed by other threads
            // simultaneously anyway.
            try {
                entry.dispose();
            } catch (Exception ex) {
                logger.error("Exception when disposing of entry {}", entry.getKey(), ex);
            }
        } finally {
            exclusiveEntryLock.unlock();
        }

        // Remove the entry from the cache. When removing, make sure that we are removing the right
        // entry to prevent case when another thread had already removed the "bad" entry and inserted
        // into the cache another, "good" one.
        this.entries.remove(entry.getKey(), entry);
    }


    private class Entry {

        // This entry is not initialized yet.
        public static final int NEW = -1;

        // This entry is already disposed of.
        public static final int DISPOSED = -2;

        private final K key;
        private final P pooledObject;

        // Number of active shared objects, and a lifecycle stage.
        // -1 (NEW): this entry has not been initialized yet. The entry must be initialized before it is able to provide
        // shared objects.
        // -2 (DISPOSED): this entry has been disposed of. A disposed entry can't be re-used. If cached, it must be
        // removed from a cache and replaced with a new entry.
        // >= 0: this entry is active (already initialized, not disposed of) and is providing this number of shared
        // objects.
        // Any other value (< -2): invalid, should never happens (indicates programming error).
        //
        // The entry may be in one of possible 3 states: NEW (-1), ACTIVE (>= 0) or DISPOSED (-2).
        // In order to change the state, this entry must be locked for the exclusive access (this.lock.writeLock),
        // other operations require shared lock (this.lock.writeLock).
        //
        // See "lock" below.
        private final AtomicInteger sharedCount = new AtomicInteger(NEW);

        // Atomic counter to create IDs for shared objects. IDs are not required for the application logic, but they
        // are written in a log to help investigate potential problems, such as when a shared reference is not
        // properly disposed of.
        private final AtomicLong sharedObjectIdGen = new AtomicLong();

        // Phanom references on shared objects. Keeping phanom references on shared objects allows to find
        // shared objects which were not properly disposed of.
        // Key: dispose callback for a shared object (runnable), value: phantom reference on shared object.
        private final ConcurrentMap<Object, SharedObjectPhantomReference<K, S>> sharedObjectPhantomRefs
                = new ConcurrentHashMap<>();

        // Lock for lifecycle management.
        // Changing the lifecycle of this entry, that is executing init() and dispose(), requires exclusive ("write")
        // lock. Other operations, such as creating or disposing of a shared object, requires shared ("read") lock.
        // The locking policy ensures that no other operations may run when the entry's lifecycle stage is changed,
        // but many non-lifecycle-related operations may run in parallel when the entry is active.
        private final ReadWriteLock lock = new ReentrantReadWriteLock();


        Entry(K key, P pooledObject) {
            this.key = Objects.requireNonNull(key);
            this.pooledObject = Objects.requireNonNull(pooledObject);
        }


        K getKey() {
            return this.key;
        }


        P getPooledObject() {
            return this.pooledObject;
        }


        public Lock sharedLock() {
            return this.lock.readLock();
        }


        public Lock exclusiveLock() {
            return this.lock.writeLock();
        }


        // Just getting a current number of shared objects does not require a lock (the variable is an atomic).
        // Note that the value may change any moment by another thread.
        public int getSharedCount() {
            return this.sharedCount.get();
        }


        public void init() throws InitializationException {
            Lock exclusiveLock = this.exclusiveLock();
            exclusiveLock.lock();
            try {
                int currentSharedCount = this.sharedCount.get();
                if (currentSharedCount >= 0) {
                    throw new IllegalStateException("Can not initialize entry " + this.key
                            + ": the entry is already initialized");
                }
                if (currentSharedCount == DISPOSED) {
                    throw new IllegalStateException("Can not initiaize entry " + this.key
                            + ": the entry is already disposed of");
                }
                assert (currentSharedCount == NEW);

                try {
                    this.pooledObject.init();
                    this.sharedCount.set(0);
                } catch (Exception ex) {
                    // An attempt to initialize this entry failed. Mark the entry as disposed and re-throw the exception.
                    this.sharedCount.set(DISPOSED);
                    throw InitializationException.wrap(this.key, ex);
                }
            } finally {
                exclusiveLock.unlock();
            }
        }


        public void dispose() {
            Lock exclusiveLock = this.exclusiveLock();
            exclusiveLock.lock();
            try {
                int currentSharedCount = this.sharedCount.get();
                if (currentSharedCount == NEW) {
                    throw new IllegalStateException("Can not dispose of entry " + this.key
                            + ": the entry is not initialized yet");
                } else if (currentSharedCount == DISPOSED) {
                    throw new IllegalStateException("Can not dispose of entry " + this.key
                            + ": the entry is already disposed of");
                }
                assert (currentSharedCount >= 0);

                try {
                    this.pooledObject.dispose();
                } catch (Exception ex) {
                    // An attempt to dispose of the pooled object failed. Log the exception, but otherwise proceed
                    // to remove the pooled object.
                    logger.error("Exception when attempting to dispose of pooled object for the entry {}, "
                            + "continue removing pooled object", this.key, ex);
                }
                this.sharedCount.set(DISPOSED);
            } finally {
                exclusiveLock.unlock();
            }
        }


        public S createSharedObject() {
            Lock sharedLock = this.sharedLock();
            sharedLock.lock();
            try {
                int currentSharedCount = this.sharedCount.get();
                if (currentSharedCount == NEW) {
                    throw new IllegalStateException("Can not create shared object from entry " + this.key
                            + ": the entry is not initialized yet");
                } else if (currentSharedCount == DISPOSED) {
                    throw new IllegalStateException("Can not create shared object from entry " + this.key
                            + ": the entry is already disposed of");
                }
                assert (currentSharedCount >= 0);

                // We have just ensured that this entry is active. Since we are holding the shared lock, the lifecycle
                // of this entry can't change (it remains active as long the lock is not released), even though
                // the number of shared objects may be changed by other threads which are also holding the shared lock.

                // Get the next ID for the new shared object. IDs are just for logging, to investigate potential
                // problems such as when a shared object is not properly disposed of.
                final long sharedObjectId = this.sharedObjectIdGen.getAndIncrement();

                // A simple value-holder is required to pass the value into a callback, because the callback has to be
                // constructed before the value is available. We may have used a 1-element array instead, but arrays
                // of generic objects are not directly supported (casting is required etc), so using a simple helper
                // class is more elegant.
                final SharedObjectPhantomReferenceHolder<K, S> sharedObjectPhantomRefHolder
                        = new SharedObjectPhantomReferenceHolder<>();

                // Create a callback for disposing the shared object directly, that is by invoking the dispose method.
                Runnable disposeDirectCallback = () -> {
                    Entry.this.disposeSharedObject(sharedObjectPhantomRefHolder.ref, true);
                };
                // Create a callback for disposing the shared object indirectly, that is from the reaper thread
                // through the phantom reference.
                Runnable disposePhantomRefCallback = () -> {
                    Entry.this.disposeSharedObject(sharedObjectPhantomRefHolder.ref, false);
                };

                // The direct callback is also used as a key in a map which contains phantom references
                // on shared objects.
                // Using the callback itself as a map key looks tricky, but actually it is not. We may have used
                // any unique object for a particular shared object (just "new Object()" would be perfectly OK)
                // as a map key, but it makes little sense to create additional objects when we already have
                // a unique dispose callback for each shared object.
                Object phantomReferenceKey = disposeDirectCallback;

                // Construct a new shared object which will invoke disposeDirectCallback when disposed.
                S sharedObject = ConcurrentSharedObjectPool.this.createSharedObject(this.pooledObject,
                        disposeDirectCallback);

                // Construct a phantom reference on the shared object and register it in the reference queue.
                // The reaper thread will invoke disposePhantomRefCallback if the shared object is not properly
                // disposed of.
                SharedObjectPhantomReference<K, S> sharedObjectPhantomRef = new SharedObjectPhantomReference<>(this.key,
                        sharedObjectId, sharedObject, disposePhantomRefCallback, phantomReferenceKey,
                        ConcurrentSharedObjectPool.this.sharedObjectsRefQueue);
                sharedObjectPhantomRefHolder.ref = sharedObjectPhantomRef;

                // Store the phantom reference on the shared object in a map, so that we may track if the shared
                // object is properly disposed of.
                this.sharedObjectPhantomRefs.put(phantomReferenceKey, sharedObjectPhantomRef);

                this.sharedCount.incrementAndGet();
                return sharedObject;
            } finally {
                sharedLock.unlock();
            }
        }


        private void disposeSharedObject(SharedObjectPhantomReference<K, S> providedPhantomRef, boolean direct) {
            // Should we offer the pool to dispose of this entry after the locked section?
            boolean offerDisposeEntry = false;

            Object phantomRefKey = providedPhantomRef.getPhantomReferenceKey();
            long sharedObjectId = providedPhantomRef.getSharedObjectId();

            Lock sharedLock = this.sharedLock();
            sharedLock.lock();
            try {
                // Synchronize on the provided phantom reference to prevent simultaneous attempts to dispose
                // of the shared object in multiple threads. This could happens in several scenarios:
                // * The most obvious scenario is if the shared object is not properly protected agains calling
                // dispose() from multiple threads.
                // * More subtle case is when this method is invoked simultaneously explicitly by the user,
                // and implicitly by the reaper thread. I have observed such behavour to happen if the shared object
                // is not used after the method dispose() is invoked, which is a pretty normal case. In such case
                // the HotSpot optimizer may find that the object is not used by the code anymore, and give it to the
                // GC even though the method dispose() is still running.
                synchronized (providedPhantomRef) {
                    // Remove the phantom reference on the shared object.
                    SharedObjectPhantomReference<K, S> sharedObjectPhantomRef =
                            this.sharedObjectPhantomRefs.remove(phantomRefKey);
                    if (sharedObjectPhantomRef != null) {
                        boolean isDisposed = sharedObjectPhantomRef.isDisposed();
                        if (isDisposed) {
                            // The shared object is still in the active map, but it is already marked as disposed.
                            // This is an unexpected case, because normally the phantom reference should have been
                            // removed from the map and marked as disposed under the synchronization lock,
                            // so this case indicates a programming error.
                            logger.error("Disposing of shared object {} / {} ({}): ref still exist, but the object "
                                    + "is already disposed by {}", this.key, sharedObjectId,
                                    (direct ? "direct" : "ref"), sharedObjectPhantomRef.getDisposedByName());
                        } else {
                            // Expected case: ref is available and is not marked as disposed.
                            // Dispose of the shared object. The return value indicates that this entry does not
                            // support any shared objects anymore, and we may offer the pool to remove this entry.
                            offerDisposeEntry = doDisposeSharedObject(sharedObjectPhantomRef, direct);
                        }
                    } else {
                        // Phantom reference is not found in the map, so the shared object is already disposed of.
                        // Below we check various cases: some of them are perfectly OK, other indicate errors.

                        // Check the reference (which we received as an argument to this method) to see if the shared
                        // object was disposed directly or indirectly.
                        boolean disposedDirect = providedPhantomRef.isDisposedDirect();
                        if (direct) {
                            if (disposedDirect) {
                                // The shared object was already directly disposed. This indicates a programming
                                // error: the same shared object was directly disposed more than once.
                                logger.warn("Disposing of shared object {} / {} ({}): the object is already disposed "
                                        + "by {}", this.key, sharedObjectId, (direct ? "direct" : "ref"),
                                        providedPhantomRef.getDisposedByName());
                            } else {
                                // The shared object was already disposed by the ripper thread through a phantom
                                // reference. I have observed such case due to Java runtime optimization.
                                // If the JVM runtime may prove that the object is not used in the subsequent code,
                                // it may give the object to the GC even though a method invoked on that object is still
                                // running. The "natural" borders of methods do not play any role, because methods
                                // may be inlined by the JVM runtime. As a workaround to prevent such false positives,
                                // one has to use the shared object after the method dispose() is called on it,
                                // but it is not elegent.
                                logger.info("Disposing of shared object {} / {} ({}): the object is already disposed "
                                        + "by the ripper thread {}. Please ignore previous warning from the ripper "
                                        + "thread about this shared object not being properly disposed of: that is "
                                        + "just a result of JVM runtime optimization", this.key, sharedObjectId,
                                        (direct ? "direct" : "ref"), providedPhantomRef.getDisposedByName());
                            }
                        } else {
                            if (disposedDirect) {
                                // Expected case: attempt to dispose shared object through phantom reference
                                // by the ripper found that the shared object already has been disposed directly.
                            } else {
                                // The shared object has already been disposed by the ripper thread. This indicates
                                // a programming error because the ripper thread should process each shared object
                                // only once.
                                logger.warn("Disposing of shared object {} / {} ({}): the object is already disposed "
                                        + "by {}", this.key, sharedObjectId, (direct ? "direct" : "ref"),
                                        providedPhantomRef.getDisposedByName());
                            }
                        }
                    }
                }
            } finally {
                sharedLock.unlock();
            }

            // If this entry is not providing any shared objects, offer to the pool to dispose of this entry.
            // It is up to the pool to decide if and when this entry should be disposed of.
            if (offerDisposeEntry) {
                ConcurrentSharedObjectPool.this.offerDisposeEntry(this);
            }
        }


        private boolean doDisposeSharedObject(SharedObjectPhantomReference<K, S> sharedObjectPhantomRef,
                boolean direct) {
            assert (Thread.holdsLock(sharedObjectPhantomRef));

            if (!direct) {
                logger.warn("Disposing of shared object {} / {} through a phantom reference. "
                        + "The shared object may have been not been properly disposed of. "
                        + "In seldom cases Java runtime optimization may cause GC to claim the object before "
                        + "the proper dispose is finished. In such case, below you should find an explanation "
                        + "from the proper dispose attempt with the same shared object ID",
                        this.key, sharedObjectPhantomRef.getSharedObjectId());
            }

            int currentSharedCount = this.sharedCount.get();
            if (currentSharedCount == NEW) {
                throw new IllegalStateException("Can not dispose of shared object from entry " + this.key
                        + ": the entry is not initialized yet");
            } else if (currentSharedCount == DISPOSED) {
                throw new IllegalStateException("Can not dispose of shared object from entry " + this.key
                        + ": the entry is already disposed of");
            } else if (currentSharedCount == 0) {
                throw new IllegalStateException("Can not dispose of shared object from entry " + this.key
                        + ": the entry has 0 shared objects");
            }
            assert (currentSharedCount > 0);

            // Mark the phantom reference as disposed.
            sharedObjectPhantomRef.markAsDisposed(direct);

            // Decrement number of shared objects provided by this entry.
            int updatedSharedCount = this.sharedCount.decrementAndGet();

            // If this entry is not providing any shared objects anymore, it may be disposed of.
            // It is up to the pool to deside if the entry should actually be disposed of.
            // A pool implementation may decide to dispose of the entry immediately, or it may decide to keep
            // an entry active for a time, so that it is immediately available if required.
            return (updatedSharedCount == 0);
        }
    }


    private static class SharedObjectPhantomReference<K, S extends SharedObject> extends PhantomReference<S> {
        // The key of the pooled object, and the ID of the shared object. Both are used only for logging.
        private final K key;
        private final long sharedObjectId;

        // A callback to be invoked to dispose of the shared object.
        private final Runnable disposeCallback;

        // A key in the entry map pointing to this phantom reference.
        private final Object phantomReferenceKey;

        // The thread which disposed of the shared object held by this phantom reference, if any.
        private Thread disposedBy;

        // Was the shared object held by this phantom reference disposed directly, that is by calling it's dispose
        // method (true) or indirectly, that is by the ripper thread cleaning up phantom references (false).
        private boolean disposedDirect;


        SharedObjectPhantomReference(K key, long sharedObjectId, S referent, Runnable disposeCallback,
                Object phantomReferenceKey, ReferenceQueue<? super S> queue) {
            super(referent, queue);
            this.key = Objects.requireNonNull(key);
            this.sharedObjectId = sharedObjectId;
            this.disposeCallback = Objects.requireNonNull(disposeCallback);
            this.phantomReferenceKey = Objects.requireNonNull(phantomReferenceKey);
        }


        public K getKey() {
            return this.key;
        }


        public long getSharedObjectId() {
            return this.sharedObjectId;
        }


        public Object getPhantomReferenceKey() {
            return this.phantomReferenceKey;
        }


        public void disposeIfRequired() {
            this.disposeCallback.run();
        }

        // Subsequent methods must be called when synchronized on this phantom reference, each of them contains
        // an assertion. We are using assertions instead of checks with runtime exceptions, because all methods
        // may be used only inside the class ConcurrentSharedObjectPool (this file), so we are in a full control.
        // Assertions are just to check for possible programming errors, especially if this class is refactored later.

        public void markAsDisposed(boolean direct) {
            assert (Thread.holdsLock(this));

            this.disposedBy = Thread.currentThread();
            this.disposedDirect = direct;
        }


        public boolean isDisposed() {
            assert (Thread.holdsLock(this));

            return (this.disposedBy != null);
        }


        public String getDisposedByName() {
            assert (Thread.holdsLock(this));

            return (this.disposedBy == null ? null : this.disposedBy.getName());
        }


        public boolean isDisposedDirect() {
            assert (Thread.holdsLock(this));

            return this.disposedDirect;
        }
    }


    // Simple mutable holder class. We may have used a 1-element array to hold the object, but arrays of generic
    // objects are not directly support and require casting, so a simple mutable object is more elegant.
    private static class SharedObjectPhantomReferenceHolder<K, S extends SharedObject> {
        public SharedObjectPhantomReference<K, S> ref;
    }
}
