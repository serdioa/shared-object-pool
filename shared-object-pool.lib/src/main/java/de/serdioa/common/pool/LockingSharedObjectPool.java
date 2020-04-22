package de.serdioa.common.pool;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


// Implementation using read/write locks.
public class LockingSharedObjectPool<K, S extends SharedObject, P> extends AbstractSharedObjectPool<K, S, P> {

    private static final Logger logger = LoggerFactory.getLogger(LockingSharedObjectPool.class);

    // Pooled entries.
    // @GuardedBy(lock)
    private final Map<K, Entry> entries = new HashMap<>();

    // Read/write lock.
    private final ReadWriteLock lock = new ReentrantReadWriteLock();


    private LockingSharedObjectPool(PooledObjectFactory<K, P> pooledObjectFactory,
            SharedObjectFactory<P, S> sharedObjectFactory,
            EvictionPolicy evictionPolicy) {
        super(pooledObjectFactory, sharedObjectFactory, evictionPolicy);
    }


    @Override
    public S get(K key) throws InvalidKeyException, InitializationException {
        while (true) {
            // Should we remove entry from the cache after the synchronized block?
            boolean removeEntryFromCache;

            // Exception thrown when attempting to initialize the entry.
            Exception exceptionOnInit = null;

            Entry entry = getEntry(key);
            Lock entryWriteLock = entry.writeLock();
            entryWriteLock.lock();
            try {
                int entrySharedCount = entry.getSharedCount();
                if (entrySharedCount >= 0) {
                    // The entry is already initialized. Just return a shared object.
                    return entry.createSharedObject();
                } else if (entrySharedCount == Entry.NEW) {
                    // The entry was just added to the pool either by this thread or by another thread.
                    // Since this thread synchronized on the entry first, we have to initialize it.
                    try {
                        entry.init();
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
                entryWriteLock.unlock();
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
    }


    private Entry getEntry(K key) throws InvalidKeyException {
        Lock poolReadLock = this.lock.readLock();
        poolReadLock.lock();
        try {
            Entry entry = this.entries.get(key);
            if (entry != null) {
                return entry;
            }
        } finally {
            poolReadLock.unlock();
        }

        Lock poolWriteLock = this.lock.writeLock();
        poolWriteLock.lock();
        try {
            Entry entry = this.entries.get(key);
            if (entry == null) {
                entry = createEntry(key);
                this.entries.put(key, entry);
            }

            return entry;
        } finally {
            poolWriteLock.unlock();
        }
    }


    private Entry createEntry(K key) throws InvalidKeyException {
        P pooledObject = createPooledObject(key);
        return new Entry(key, pooledObject);
    }


    // Entry offer the pool to dispose of itself.
    // It is up to the eviction policy to decide if the offer to be accepted immediately, or to dispose of the entry
    // later.
    // If the eviction policy decides to postpone, it may happens that in the meantime the entry will be re-used
    // and not anymore eligible to disposal when the eviction policy decided to do so.
    private void entryOfferDispose(Entry entry) {
        Cancellable evictionCancellable = this.evictionPolicy.evict(() -> this.evictionPolicyOfferDispose(entry));

        // TODO: store evictionCancellable in Entry, and make sure Entry cancels it if it is not immediately
        // disposed of, and in the meantime becomes non-eligible for a disposal.
    }


    // Eviction policy offer the pool to dispose of the entry.
    // It could be that in the meantime the entry is not eligible for a disposal anymore.
    private void evictionPolicyOfferDispose(Entry entry) {
        Lock entryWriteLock = entry.writeLock();
        entryWriteLock.lock();
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

            // Dispose of the entry. Note that the entry still remains in the cache.
            try {
                entry.dispose();
            } catch (Exception ex) {
                logger.error("Exception when disposing of entry {}", entry.getKey(), ex);
            }
        } finally {
            entryWriteLock.unlock();
        }

        // Remove the entry from the cache. When removing, make sure that we are removing the right
        // entry to prevent case when another thread had already removed the "bad" entry and inserted
        // into the cache another, "good" one.
        Lock poolWriteLock = this.lock.writeLock();
        poolWriteLock.lock();
        try {
            this.entries.remove(entry.getKey(), entry);
        } finally {
            poolWriteLock.unlock();
        }
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
        // @GuardedBy(this)
        private int sharedCount = NEW;

        // Read/write lock.
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


        public Lock readLock() {
            return this.lock.readLock();
        }


        public Lock writeLock() {
            return this.lock.writeLock();
        }


        void init() throws InitializationException {
            Lock entryWriteLock = this.lock.writeLock();
            entryWriteLock.lock();
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
                entryWriteLock.unlock();
            }
        }


        void dispose() {
            Lock entryWriteLock = this.lock.writeLock();
            entryWriteLock.lock();
            try {
                ensureActive();

                LockingSharedObjectPool.this.disposePooledObject(this.pooledObject);
                this.sharedCount = DISPOSED;
            } finally {
                entryWriteLock.unlock();
            }
        }


        int getSharedCount() {
            Lock entryReadLock = this.lock.readLock();
            entryReadLock.lock();
            try {
                return this.sharedCount;
            } finally {
                entryReadLock.unlock();
            }
        }


        S createSharedObject() {
            Lock entryWriteLock = this.lock.writeLock();
            entryWriteLock.lock();
            try {
                ensureActive();

                S sharedObject = LockingSharedObjectPool.this
                        .createSharedObject(this.pooledObject, this::disposeSharedObject);

                this.sharedCount++;
                return sharedObject;
            } finally {
                entryWriteLock.unlock();
            }
        }


        private void disposeSharedObject() {
            // Should we offer the pool to dispose of this entry after the synchronized block?
            boolean offerDisposeEntry = false;

            Lock entryWriteLock = this.lock.writeLock();
            entryWriteLock.lock();
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
                }
            } finally {
                entryWriteLock.unlock();
            }

            if (offerDisposeEntry) {
                LockingSharedObjectPool.this.entryOfferDispose(this);
            }
        }


        private void ensureNew() {
            assert (this.lock.isWriteLockedByCurrentThread());

            if (this.sharedCount != NEW) {
                throw new IllegalStateException("Entry is already initialized");
            }
        }


        private void ensureActive() {
            assert (this.lock.isWriteLockedByCurrentThread());

            if (this.sharedCount == NEW) {
                throw new IllegalStateException("Entry is not initialized yet");
            }
            if (this.sharedCount == DISPOSED) {
                throw new IllegalStateException("Entry is already disposed of");
            }
        }
    }


    public static class Builder<K, S extends SharedObject, P> {

        // Factory for creating new pooled objects.
        protected PooledObjectFactory<K, P> pooledObjectFactory;

        // Factory for creating shared objects from pooled objects.
        protected SharedObjectFactory<P, S> sharedObjectFactory;

        // The policy for evicting non-used pooled objects.
        private EvictionPolicy evictionPolicy;


        public Builder<K, S, P> setPooledObjectFactory(PooledObjectFactory<K, P> pooledObjectFactory) {
            this.pooledObjectFactory = pooledObjectFactory;
            return this;
        }


        public Builder<K, S, P> setSharedObjectFactory(SharedObjectFactory<P, S> sharedObjectFactory) {
            this.sharedObjectFactory = sharedObjectFactory;
            return this;
        }


        public Builder<K, S, P> setEvictionPolicy(EvictionPolicy evictionPolicy) {
            this.evictionPolicy = evictionPolicy;
            return this;
        }


        public LockingSharedObjectPool<K, S, P> build() {
            if (this.pooledObjectFactory == null) {
                throw new IllegalStateException("pooledObjectFactory is required");
            }
            if (this.sharedObjectFactory == null) {
                throw new IllegalStateException("sharedObjectFactory is required");
            }
            if (this.evictionPolicy == null) {
                throw new IllegalStateException("evictionPolicy is required");
            }

            return new LockingSharedObjectPool<>(this.pooledObjectFactory, this.sharedObjectFactory,
                    this.evictionPolicy);
        }
    }
}
