package de.serdioa.common.pool;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An implementation of an {@link SharedObjectPool} using locks. This implementation scales very bad with a number of
 * threads and is not recommended to be used. It is provided just for comparison with other implementations.
 *
 * @param <K> the type of keys used to access shared objects provided by this pool.
 * @param <S> the type of shared objects provided by this pool.
 * @param <P> the type of implementation objects backing shared objects provided by this pool.
 */
public class LockingSharedObjectPool<K, S extends SharedObject, P> extends AbstractSharedObjectPool<K, S, P> {

    private static final Logger logger = LoggerFactory.getLogger(LockingSharedObjectPool.class);

    // Pooled entries.
    // @GuardedBy(lock)
    private final Map<K, Entry> entries = new HashMap<>();

    // Is this object pool already disposed of?
    // @GuardedBy(lock)
    private boolean disposed = false;

    // Synchronization lock.
    private final ReadWriteLock lock = new ReentrantReadWriteLock();


    private LockingSharedObjectPool(String name,
            PooledObjectFactory<K, P> pooledObjectFactory,
            SharedObjectFactory<P, S> sharedObjectFactory,
            boolean disposeUnused,
            long idleDisposeTimeMillis,
            int disposeThreads) {
        super(name, pooledObjectFactory, sharedObjectFactory, disposeUnused, idleDisposeTimeMillis, disposeThreads);
    }


    @Override
    public S get(K key) throws InvalidKeyException, InitializationException {
        long startGetTimestamp = System.nanoTime();
        boolean poolHit = false;

        try {
            while (true) {
                // Should we remove entry from the cache after the synchronized block?
                boolean removeEntryFromCache;

                // Exception thrown when attempting to initialize the entry.
                Exception exceptionOnInit = null;

                Entry entry = getEntry(key);

                // Duration statistics collected if we actually initialize a new pooled object.
                boolean attemptedInitialize = false;
                long startInitializeTimestamp = Long.MIN_VALUE;
                long endInitializeTimestamp = Long.MIN_VALUE;
                boolean initializeSuccess = false;

                Lock entryExclusiveLock = entry.exclusiveLock();
                entryExclusiveLock.lock();
                try {
                    int entrySharedCount = entry.getSharedCount();
                    if (entrySharedCount >= 0) {
                        // The entry is already initialized. Just return a shared object.
                        poolHit = true;
                        return entry.createSharedObject();
                    } else if (entrySharedCount == Entry.NEW) {
                        // The entry was just added to the pool either by this thread or by another thread.
                        // Since this thread synchronized on the entry first, we have to initialize it.
                        poolHit = false;
                        attemptedInitialize = true;
                        startInitializeTimestamp = System.nanoTime();
                        try {
                            entry.init();
                            endInitializeTimestamp = System.nanoTime();
                            initializeSuccess = true;

                            return entry.createSharedObject();
                        } catch (Exception ex) {
                            // An attempt to initialize this entry failed. Process this case after releasing
                            // the synchronization lock on entry.
                            exceptionOnInit = ex;
                            removeEntryFromCache = true;
                        }
                    } else {
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
                        // when the entry is already disposed of. In such case we will remove the disposed entry
                        // from the cache, and try once more.
                        removeEntryFromCache = true;
                    }
                } finally {
                    entryExclusiveLock.unlock();

                    if (attemptedInitialize) {
                        // We have attempted to initialize a new pooled object. Were we successfull? If not, take the
                        // end timestamp.
                        if (!initializeSuccess) {
                            endInitializeTimestamp = System.nanoTime();
                        }
                        this.firePooledObjectInitialized(endInitializeTimestamp - startInitializeTimestamp,
                                initializeSuccess);
                    }
                }

                if (removeEntryFromCache) {
                    // Either the entry is already disposed of, or an attempt to initialize the entry failed.
                    // Remove the entry from the cache. When removing, make sure that we are removing the right
                    // entry to prevent case when another thread had already removed the "bad" entry and inserted
                    // into the cache another, "good" one.
                    Lock poolWriteLock = this.lock.writeLock();
                    poolWriteLock.lock();
                    try {
                        this.entries.remove(key, entry);
                    } finally {
                        poolWriteLock.unlock();
                    }
                }

                if (exceptionOnInit != null) {
                    // If an attempt to initialize an entry caused an exception, we will not attempt to create
                    // and initialize an entry again, because it could cause an infinite loop. Instead, we are re-throwing
                    // the exception.
                    throw InitializationException.wrap(key, exceptionOnInit);
                }
            }
        } finally {
            long endGetTimestamp = System.nanoTime();
            this.fireSharedObjectGet(endGetTimestamp - startGetTimestamp, poolHit);
        }
    }


    private Entry getEntry(K key) throws InvalidKeyException {
        // Optimistically attempt to get an entry under a shared lock. If the entry already exist in the pool, we do not
        // need an exclusive lock.
        Lock poolReadLock = this.lock.readLock();
        poolReadLock.lock();
        try {
            // Before doing anything, check if this pool is already shutting down.
            if (this.disposed) {
                throw new IllegalStateException("The pool is already disposed of");
            }

            Entry entry = this.entries.get(key);
            if (entry != null) {
                return entry;
            }
        } finally {
            poolReadLock.unlock();
        }

        // We have not found an entry in the pool under a shared lock. Now we get an exclusive lock, and we will create
        // an entry if it does not exist. We still have to check whether an entry exist, because it may have been
        // created by another thread in the meantime.
        // Duration statistics collected if we actually create a new pooled object.
        boolean attemptedCreate = false;
        long startCreateTimestamp = Long.MIN_VALUE;
        long endCreateTimestamp = Long.MIN_VALUE;
        boolean createSuccess = false;

        Lock poolWriteLock = this.lock.writeLock();
        poolWriteLock.lock();
        try {
            // Once more, before doing anything, check if this pool is already shutting down.
            // This pool may have been disposed of between the read lock above and the write lock we are holding now.
            if (this.disposed) {
                throw new IllegalStateException("The pool is already disposed of");
            }

            Entry entry = this.entries.get(key);
            if (entry == null) {
                attemptedCreate = true;
                startCreateTimestamp = System.nanoTime();
                entry = createEntry(key);
                endCreateTimestamp = System.nanoTime();
                createSuccess = true;

                this.entries.put(key, entry);
            }

            return entry;
        } finally {
            poolWriteLock.unlock();

            if (attemptedCreate) {
                // We have attempted to create a new pooled object. Were we successfull? If not, take the end timestamp.
                if (!createSuccess) {
                    endCreateTimestamp = System.nanoTime();
                }
                this.firePooledObjectCreated(endCreateTimestamp - startCreateTimestamp, createSuccess);
            }
        }
    }


    private Entry createEntry(K key) throws InvalidKeyException {
        P pooledObject = createPooledObject(key);
        return new Entry(key, pooledObject);
    }


    // Try to dispose of the specified entry. We may dispose the entry synchronously, or schedule a later attempt
    // dependend on the configuration of this shared object pool.
    // It could be that in the meantime the entry is not eligible for a disposal anymore, in such case no disposal
    // takes place.
    private void offerDispose(Entry entry) {
        if (!this.disposeUnused) {
            // Fast track if disposing of unused entries is disabled.
            return;
        }

        // Should we remove entry from the cache after the synchronized block?
        boolean removeEntryFromCache;

        // Duration statistics collected if we actually dispose of the pooled object.
        long startDisposeTimestamp = Long.MIN_VALUE;
        long endDisposeTimestamp = Long.MIN_VALUE;
        boolean disposeSuccess = false;

        Lock entryExclusiveLock = entry.exclusiveLock();
        entryExclusiveLock.lock();
        try {
            // An entry may be disposed of only if it is not providing any shared objects. Since this method
            // is called without holding a lock on entry, it could be that in the meantime another thread acquired
            // a new shared object from the entry.
            int entrySharedCount = entry.getSharedCount();
            if (entrySharedCount > 0) {
                // The entry is providing some shared objects, so it can't be disposed of.
                return;
            } else if (entrySharedCount == Entry.DISPOSED) {
                // Another thread had already disposed of the entry. This could happens in the following scenario:
                // * Another thread (B) released the last shared object from the entry, and called this method
                // to offer the entry for disposal.
                // * Before another thread (B) is able to obtain a synchronization lock, this thread (A) obtains
                // the lock, acquires a new shared object, releases the shared object, and calls this method to offer
                // the entry for disposal.
                // * Another thread (B) obtain the synchronization lock and disposes of the entry.
                // * This thread (A) obtains the synchronization lock, but the entry is already disposed of.
                return;
            } else if (entrySharedCount == Entry.NEW) {
                throw new IllegalStateException("Entry offered for disposal, but the status is new: " + entry.getKey());
            }

            // The entry is eligible for disposal. Shall we dispose of the entry immediately, or shall we schedule it
            // to be disposed later?
            boolean disposeEntryImmediately;
            if (this.idleDisposeTimeMillis > 0) {
                long lastReturnTime = entry.getLastReturnTime();
                long disposeAt = lastReturnTime + this.idleDisposeTimeMillis;
                long now = System.currentTimeMillis();
                long delayBeforeDispose = disposeAt - now;

                if (delayBeforeDispose > 0) {
                    // Instead of disposing of the entry immediately, schedule to try again later.
                    ScheduledFuture<?> disposeTask = this.scheduleDisposeTask(() -> this.offerDispose(entry),
                            delayBeforeDispose, TimeUnit.MILLISECONDS);
                    entry.setDisposeTask(disposeTask);

                    // We have scheduled a later attempt.
                    disposeEntryImmediately = false;
                } else {
                    // Generally asynchronous disposal of entries is configured, but for this entry the idle time
                    // already expired and we shall dispose of it immediately.
                    disposeEntryImmediately = true;
                }
            } else {
                // A synchronous disposal of entries is configured, we never wait.
                disposeEntryImmediately = true;
            }

            // Dispose of the entry. Note that the entry still remains in the cache.
            if (disposeEntryImmediately) {
                startDisposeTimestamp = System.nanoTime();
                try {
                    entry.dispose(false);
                    endDisposeTimestamp = System.nanoTime();
                    disposeSuccess = true;
                } catch (Exception ex) {
                    endDisposeTimestamp = System.nanoTime();
                    disposeSuccess = false;
                    logger.error("Exception when disposing of entry {}", entry.getKey(), ex);
                }

                // We have disposed of the entry, so we shall remove it from cache after the synchronized block.
                removeEntryFromCache = true;
            } else {
                // We have not disposed of the entry: either it is not eligible for the disposal at all (for example,
                // because it provides shared objects), or we have scheduled the entry for a later disposal.
                // Either way, the entry shall remain in the cache.
                removeEntryFromCache = false;
            }
        } finally {
            entryExclusiveLock.unlock();
        }

        // Remove the entry from the cache, if it has been disposed of. When removing, make sure that we are removing
        // the right entry to prevent case when another thread had already removed the "bad" entry and inserted into
        // the cache another, "good" one.
        if (removeEntryFromCache) {
            Lock poolWriteLock = this.lock.writeLock();
            poolWriteLock.lock();
            try {
                this.entries.remove(entry.getKey(), entry);
            } finally {
                poolWriteLock.unlock();
            }

            // Since we have evicted the entry, we have disposed of the pooled object, either successfully
            // or with a failure. Anyway, we may notify statistics listeners.
            this.firePooledObjectDisposed(endDisposeTimestamp - startDisposeTimestamp, disposeSuccess);
        }
    }


    @Override
    public int getPooledObjectsCount() {
        Lock poolReadLock = this.lock.readLock();
        poolReadLock.lock();
        try {
            return this.entries.size();
        } finally {
            poolReadLock.unlock();
        }
    }


    @Override
    public int getUnusedPooledObjectsCount() {
        Lock poolReadLock = this.lock.readLock();
        poolReadLock.lock();
        try {
            int unusedPooledObjectsCount = 0;
            for (Entry entry : this.entries.values()) {
                if (entry.getSharedCount() <= 0) {
                    unusedPooledObjectsCount++;
                }
            }

            return unusedPooledObjectsCount;
        } finally {
            poolReadLock.unlock();
        }
    }


    @Override
    public int getSharedObjectsCount() {
        Lock poolReadLock = this.lock.readLock();
        poolReadLock.lock();
        try {
            int sharedObjectsCount = 0;
            for (Entry entry : this.entries.values()) {
                if (entry.getSharedCount() > 0) {
                    sharedObjectsCount += Math.max(0, entry.getSharedCount());
                }
            }

            return sharedObjectsCount;
        } finally {
            poolReadLock.unlock();
        }
    }


    @Override
    public int getSharedObjectsCount(K key) {
        Entry entry;

        Lock poolReadLock = this.lock.readLock();
        poolReadLock.lock();
        try {
            entry = this.entries.get(key);
        } finally {
            poolReadLock.unlock();
        }

        return (entry == null ? 0 : Math.max(0, entry.getSharedCount()));
    }


    public boolean containsPooledObject(K key) {
        Lock poolReadLock = this.lock.readLock();
        poolReadLock.lock();
        try {
            return this.entries.containsKey(key);
        } finally {
            poolReadLock.unlock();
        }
    }


    @Override
    public void dispose() {
        Lock poolWriteLock = this.lock.writeLock();
        poolWriteLock.lock();
        try {
            // Fast-track if this pool is already disposed of.
            if (this.disposed) {
                return;
            }

            // Mark this pool as disposed.
            this.disposed = true;
        } finally {
            poolWriteLock.unlock();
        }

        this.disposeEntriesOnShutdown();

        super.dispose();
    }


    // Dispose of all still available entries when this pool is disposed of.
    // The implementation below looks a bit convoluted, but it is required to report performance metrics
    // when disposing of entries, without calling external methods (metrics listeners) in a synchronized block.
    private void disposeEntriesOnShutdown() {
        Lock poolReadLock = this.lock.readLock();

        while (true) {
            Optional<Entry> entryHolder;

            poolReadLock.lock();
            try {
                entryHolder = this.entries.values().stream().findAny();
            } finally {
                poolReadLock.unlock();
            }

            if (entryHolder.isPresent()) {
                Entry entry = entryHolder.get();
                disposeEntryOnShutdown(entry);
            } else {
                // There are no entries in this pool anymore. Terminate the "while" loop.
                break;
            }
        }
    }


    // Dispose of the specified entry when this pool is disposed of.
    private void disposeEntryOnShutdown(Entry entry) {
        long startDisposeTimestamp = System.nanoTime();
        long endDisposeTimestamp;
        boolean disposeSuccess;
        try {
            entry.dispose(true);
            endDisposeTimestamp = System.nanoTime();
            disposeSuccess = true;
        } catch (Exception ex) {
            endDisposeTimestamp = System.nanoTime();
            disposeSuccess = false;
            logger.error("Exception when disposing of entry {}", entry.getKey(), ex);
        }

        Lock poolWriteLock = this.lock.writeLock();
        poolWriteLock.lock();
        try {
            this.entries.remove(entry.getKey(), entry);
        } finally {
            poolWriteLock.unlock();
        }

        this.firePooledObjectDisposed(endDisposeTimestamp - startDisposeTimestamp, disposeSuccess);
    }


    private class Entry {

        // This entry is not initialized yet.
        public static final int NEW = -1;

        // This entry is already disposed of.
        public static final int DISPOSED = -2;

        private final K key;
        private final P pooledObject;

        // Number of active shared objects.
        // Negative value means that this entry is not initialized.
        // @GuardedBy(this.lock)
        private int sharedCount = NEW;

        // Last time the last shared object from this entry was returned. The time makes sense only if this entry
        // currently does not provide any shared objects, that is if sharedCount == 0, because the time is NOT reset
        // when new shared objects are created.
        // @GuardedBy(this.lock)
        private long lastReturnTime = 0;

        // The ScheduledFuture for asynchronously disposing of this entry, if any.
        // A ScheduledFuture is set when this entry is scheduled for an asynchronous disposal.
        // If this entry provides a new shared object before being disposed, it may cancel the ScheduledFuture.
        // Even if the ScheduledFuture is not cancellled, it will not dispose of this entry when executed, if the
        // entry provides a shared object.
        // @GuardedBy(this.lock)
        private ScheduledFuture<?> disposeTask;

        // Lock for lifecycle management.
        // Changing the lifecycle of this entry, that is executing init() and dispose(), requires exclusive ("write")
        // lock. Other operations, such as creating or disposing of a shared object, requires shared ("read") lock.
        // The locking policy ensures that no other operations may run when the entry's lifecycle stage is changed,
        // but many non-lifecycle-related operations may run in parallel when the entry is active.
        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();


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


        void init() throws InitializationException {
            Lock entryExclusiveLock = this.exclusiveLock();
            entryExclusiveLock.lock();
            try {
                ensureNew();

                try {
                    LockingSharedObjectPool.this.initializePooledObject(this.pooledObject);
                    this.sharedCount = 0;
                } catch (Exception ex) {
                    // An attempt to initialize this entry failed. Mark the entry as disposed and re-throw the exception.
                    this.sharedCount = DISPOSED;
                    throw InitializationException.wrap(this.key, ex);
                }
            } finally {
                entryExclusiveLock.unlock();
            }
        }


        // Dispose of this entry, that is dispose of the underlying pooled object and mark this entry as disposed.
        // If the parameter onShutdown is true, it indicates this method is called when shutting down the whole pool.
        // When this method is called with onShutdown = false, it ensures that the lifecycle stage is respected,
        // that is it disposes of this entry only if the entry is active, but does not provide any shared objects.
        // If the parameter onShutdown = true, this method will dispose of this entry regardless of the lifecycle stage.
        void dispose(boolean onShutdown) {
            Lock entryExclusiveLock = this.exclusiveLock();
            entryExclusiveLock.lock();
            try {
                if (!onShutdown) {
                    // This is an attempt to dispose of this entry in a normal case, that is in a running pool.
                    // Check that the entry may be disposed of, that is the entry is active, but does not provide any shared
                    // objects.
                    if (this.sharedCount == NEW) {
                        throw new IllegalStateException("Entry is not initialized yet");
                    } else if (this.sharedCount == DISPOSED) {
                        throw new IllegalStateException("Entry is already disposed of");
                    } else if (this.sharedCount > 0) {
                        throw new IllegalStateException("Entry provides " + this.sharedCount + " shared objects");
                    }
                }

                // Dispose of the underlying pool object, if it is active, that is if it was initialized,
                // but not disposed of.
                if (this.sharedCount >= 0) {
                    LockingSharedObjectPool.this.disposePooledObject(this.pooledObject);
                }
                this.sharedCount = DISPOSED;
            } finally {
                entryExclusiveLock.unlock();
            }
        }


        int getSharedCount() {
            Lock entrySharedLock = this.sharedLock();
            entrySharedLock.lock();
            try {
                return this.sharedCount;
            } finally {
                entrySharedLock.unlock();
            }
        }


        long getLastReturnTime() {
            Lock entrySharedLock = this.sharedLock();
            entrySharedLock.lock();
            try {
                return this.lastReturnTime;
            } finally {
                entrySharedLock.unlock();
            }
        }


        void setDisposeTask(ScheduledFuture<?> disposeTask) {
            Lock entryExclusiveLock = this.exclusiveLock();
            entryExclusiveLock.lock();
            try {
                this.disposeTask = disposeTask;
            } finally {
                entryExclusiveLock.unlock();
            }
        }


        S createSharedObject() {
            Lock entrySharedLock = this.sharedLock();
            entrySharedLock.lock();
            try {
                ensureActive();

                S sharedObject = LockingSharedObjectPool.this
                        .createSharedObject(this.pooledObject, this::disposeSharedObject);

                this.sharedCount++;

                // If this entry was scheduled for disposal, attempt to cancel the dispose task.
                // This entry will not be disposed of even if the task can not be cancelled (the task will not dispose
                // of this entry if it provides any shared objects), but cancelling the task reduces unnecessary load
                // on the disposal executor.
                if (this.disposeTask != null) {
                    this.disposeTask.cancel(true);
                    this.disposeTask = null;
                }

                return sharedObject;
            } finally {
                entrySharedLock.unlock();
            }
        }


        private void disposeSharedObject() {
            // Should we offer the pool to dispose of this entry after the synchronized block?
            boolean offerDisposeEntry = false;

            Lock entrySharedLock = this.sharedLock();
            entrySharedLock.lock();
            try {
                ensureActive();

                if (this.sharedCount == 0) {
                    logger.warn("Disposed of a shared object {}, but shared object counter is 0", this.key);
                } else {
                    this.sharedCount--;
                }

                if (this.sharedCount == 0) {
                    // This entry is not providing any shared objects anymore, and may be disposed of.
                    // It is up to the pool to deside if the entry should actually be disposed of.
                    // A pool implementation may decide to dispose of the entry immediately, or it may decide to keep
                    // an entry active for a time, so that it is immediately available if required.
                    offerDisposeEntry = true;

                    // Set the time when the last shared object was returned.
                    this.lastReturnTime = System.currentTimeMillis();
                }
            } finally {
                entrySharedLock.unlock();
            }

            if (offerDisposeEntry) {
                LockingSharedObjectPool.this.offerDispose(this);
            }
        }


        private void ensureNew() {
            assert (this.lock.isWriteLockedByCurrentThread());

            if (this.sharedCount != NEW) {
                throw new IllegalStateException("Entry is already initialized");
            }
        }


        private void ensureActive() {
            if (this.sharedCount == NEW) {
                throw new IllegalStateException("Entry is not initialized yet");
            }
            if (this.sharedCount == DISPOSED) {
                throw new IllegalStateException("Entry is already disposed of");
            }
        }
    }


    public static class Builder<K, S extends SharedObject, P> extends AbstractSharedObjectPool.Builder<K, S, P, Builder<K, S, P>> {

        public LockingSharedObjectPool<K, S, P> build() {
            this.validate();

            return new LockingSharedObjectPool<>(this.name, this.pooledObjectFactory, this.sharedObjectFactory,
                    this.disposeUnused, this.idleDisposeTimeMillis, this.disposeThreads);
        }
    }
}
