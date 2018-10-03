package de.serdioa.common.pool;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
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

    // A queue with weak references on shared objects. We keep them to be able to find shared objects which were not
    // properly disposed of.
    private final ReferenceQueue<S> sharedObjectsRefQueue = new ReferenceQueue<>();

    // The thread processing weak references on shared objects claimed by the GC.
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
                SharedObjectWeakReference<?, ? extends S> ref
                        = (SharedObjectWeakReference<?, ? extends S>) this.sharedObjectsRefQueue.remove();
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

        // Weak references on shared objects. Keeping weak references on shared objects allows to find shared objects
        // which were not properly disposed of.
        // Key: dispose callback for a shared object (runnable), value: weak reference on shared object.
        private final AtomicLong sharedObjectIdGen = new AtomicLong();
        private final ConcurrentMap<Object, SharedObjectWeakReference<K, S>> sharedObjectWeakRefs
                = new ConcurrentHashMap<>();
        private final ConcurrentMap<Object, SharedObjectWeakReference<K, S>> disposedSharedObjectWeakRefs
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

                // Create a callback called when the shared object is disposed. The callback is also used as a key
                // in a map which contains weak references on shared objects. In order to remove the weak reference
                // from the map when the shared object is disposed of, the callback requires a reference to itself
                // (as a map key).
                // Using the callback itself as a map key looks tricky, but actually it is not. We may have used
                // any unique object for a particular shared object (just "new Object()" would be perfectly OK)
                // as a map key, but it makes little sense to create additional objects when we already have
                // a unique dispose callback for each shared object.
                final long sharedObjectId = this.sharedObjectIdGen.getAndIncrement();
                final SharedObjectWeakReferenceHolder<K, S> sharedObjectWeakRefHolder
                        = new SharedObjectWeakReferenceHolder<>();
                Runnable disposeDirectCallback = new Runnable() {
                    @Override
                    public void run() {
                        Entry.this.disposeSharedObject(this, sharedObjectId, true, sharedObjectWeakRefHolder.ref);
                    }
                };
                Runnable disposeWeakRefCallback = new Runnable() {
                    @Override
                    public void run() {
                        Entry.this.disposeSharedObject(disposeDirectCallback, sharedObjectId, false, sharedObjectWeakRefHolder.ref);
                    }
                };
                S sharedObject = ConcurrentSharedObjectPool.this
                        .createSharedObject(this.pooledObject, disposeDirectCallback);
                this.sharedCount.incrementAndGet();

                SharedObjectWeakReference<K, S> sharedObjectWeakRef = new SharedObjectWeakReference<>(this.key,
                        sharedObjectId, sharedObject, disposeWeakRefCallback,
                        ConcurrentSharedObjectPool.this.sharedObjectsRefQueue);
                sharedObjectWeakRefHolder.ref = sharedObjectWeakRef;
                this.sharedObjectWeakRefs.put(disposeDirectCallback, sharedObjectWeakRef);

                return sharedObject;
            } finally {
                sharedLock.unlock();
            }
        }


        private void disposeSharedObject(Object weakRefKey, long sharedObjectId, boolean direct,
                SharedObjectWeakReference<K, S> providedWeakRef) {
            // Should we offer the pool to dispose of this entry after the locked section?
            boolean offerDisposeEntry = false;

            if (providedWeakRef == null) {
                logger.error("Disposing of shared object {} / {} ({}): provided weak ref is null",
                        this.key, sharedObjectId, (direct ? "direct" : "ref"));
            }

            Lock sharedLock = this.sharedLock();
            sharedLock.lock();
            try {
                synchronized (providedWeakRef) {
                    // Remove the weak reference on the shared object.
                    SharedObjectWeakReference<K, S> sharedObjectWeakRef = this.sharedObjectWeakRefs.remove(weakRefKey);
                    if (sharedObjectWeakRef != null) {
                        boolean isDisposed = sharedObjectWeakRef.isDisposed();
                        if (isDisposed) {
                            // The shared object is still in the active map, but is marked as disposed.
                            boolean availableFromRef = (sharedObjectWeakRef.get() != null);
                            logger.warn("Disposing of shared object {} / {} ({}): ref still exist, but the object "
                                    + "is already disposed by {}, availableFromRef={}", this.key, sharedObjectId, (direct ? "direct" : "ref"),
                                    sharedObjectWeakRef.getDisposedByName(), availableFromRef);
                        } else {
                            // Expected case: ref is available and is not marked as disposed.
                            offerDisposeEntry = doDisposeSharedObject(sharedObjectWeakRef, weakRefKey, direct);
                        }
                    } else {
                        // Ref is not found. Check in the map with disposed refs.
                        SharedObjectWeakReference<K, S> disposedWeakRef = this.disposedSharedObjectWeakRefs
                                .get(weakRefKey);
                        if (disposedWeakRef != null) {
                            boolean disposedDirect = disposedWeakRef.isDisposedDirect();
                            if (direct || !disposedDirect) {
                                boolean availableFromRef = (disposedWeakRef.get() != null);
                                logger.warn("Disposing of shared object {} / {} ({}): ref does not exist, the object "
                                        + "is already disposed by {}, availableFromRef={}", this.key, sharedObjectId, (direct ? "direct" : "ref"),
                                        disposedWeakRef.getDisposedByName(), availableFromRef, new Exception("Stack Trace"));
                            } else {
                                // Expected case: attempt to dispose weak ref by ripper found that it was already
                                // disposed directly.
                            }
                        } else {
                            boolean availableFromRef = (providedWeakRef.get() != null);
                            logger.warn("Disposing of shared object {} / {} ({}): ref does not exist, the object "
                                    + "can't be found amongst disposed, availableFromRef={}", this.key, sharedObjectId,
                                    (direct ? "direct" : "ref"), availableFromRef);
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


        private boolean doDisposeSharedObject(SharedObjectWeakReference<K, S> sharedObjectWeakRef,
                Object weakRefKey, boolean direct) {
            assert (Thread.holdsLock(sharedObjectWeakRef));

            if (!direct) {
                logger.warn("Disposing of shared object {} / {} through a weak reference. "
                        + "The shared object may have been not been properly disposed of.",
                        this.key, sharedObjectWeakRef.getId());
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

            sharedObjectWeakRef.markAsDisposed(direct);
            this.disposedSharedObjectWeakRefs.put(weakRefKey, sharedObjectWeakRef);

            int updatedSharedCount = this.sharedCount.decrementAndGet();

            // If this entry is not providing any shared objects anymore, it may be disposed of.
            // It is up to the pool to deside if the entry should actually be disposed of.
            // A pool implementation may decide to dispose of the entry immediately, or it may decide to keep
            // an entry active for a time, so that it is immediately available if required.
            return (updatedSharedCount == 0);
        }
    }


    private static class SharedObjectWeakReference<K, S extends SharedObject> extends WeakReference<S> {

        private final K key;
        private final long id;
        private final Runnable disposeCallback;
        private Thread disposedBy;
        private boolean disposedDirect;


        SharedObjectWeakReference(K key, long id, S referent, Runnable disposeCallback,
                ReferenceQueue<? super S> queue) {
            super(referent, queue);
            this.key = Objects.requireNonNull(key);
            this.id = id;
            this.disposeCallback = Objects.requireNonNull(disposeCallback);
        }


        public K getKey() {
            return this.key;
        }


        public long getId() {
            return this.id;
        }


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


        public void disposeIfRequired() {
            this.disposeCallback.run();
        }
    }


    private static class SharedObjectWeakReferenceHolder<K, S extends SharedObject> {
        public SharedObjectWeakReference<K, S> ref;
    }
}
