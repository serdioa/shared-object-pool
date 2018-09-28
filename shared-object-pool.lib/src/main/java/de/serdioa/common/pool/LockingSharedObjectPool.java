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
public class LockingSharedObjectPool<K, S extends SharedObject, P extends PooledObject> implements SharedObjectPool<K, S> {
    private static final Logger logger = LoggerFactory.getLogger(LockingSharedObjectPool.class);

    // Factory for creating new pooled objects.
    private PooledObjectFactory<K, P> pooledObjectFactory;

    // Factory for creating shared objects from pooled objects.
    private SharedObjectFactory<P, S> sharedObjectFactory;

    // Pooled entries.
    // @GuardedBy(lock)
    private final Map<K, Entry> entries = new HashMap<>();

    // Read/write lock.
    private final ReadWriteLock lock = new ReentrantReadWriteLock();


    public void setPooledObjectFactory(PooledObjectFactory<K, P> factory) {
        this.pooledObjectFactory = Objects.requireNonNull(factory);
    }


    public void setSharedObjectFactory(SharedObjectFactory<P, S> factory) {
        this.sharedObjectFactory = Objects.requireNonNull(factory);
    }


    @Override
    public S get(K key) throws InvalidKeyException, InitializationException {
        while (true) {
            // Should we remove entry from the cache after the synchronized block?
            boolean removeEntryFromCache;

            // Exception thrown when attempting to initialize the entry.
            Exception exceptionOnInit = null;

            Entry entry = getEntry(key);
            synchronized (entry) {
                int entrySharedCount = entry.getSharedCount();
                if (entrySharedCount >= 0) {
                    // The entry is already initialized. Just return a shared object.
                    return entry.createSharedObject();
                } else if  (entrySharedCount == Entry.NEW) {
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
            } // Releasing synchronization lock on entry.

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
        // TODO: try first check in the read lock, and only afterwards in the write lock.

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


    protected P createPooledObject(K key) throws InvalidKeyException {
        return this.pooledObjectFactory.create(key);
    }


    protected S createSharedObject(P pooledObject, Runnable disposeCallback) {
        return this.sharedObjectFactory.createShared(pooledObject, disposeCallback);
    }


    private void offerDisposeEntry(Entry entry) {
        synchronized (entry) {
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


        synchronized void init() throws InitializationException {
            ensureNew();

            try {
                this.pooledObject.init();
                this.sharedCount = 0;
            } catch (Exception ex) {
                // An attempt to initialize this entry failed. Mark the entry as disposed and re-throw the exception.
                this.sharedCount = DISPOSED;
                throw InitializationException.wrap(this.key, ex);
            }
        }


        synchronized void dispose() {
            ensureActive();

            this.pooledObject.dispose();
            this.sharedCount = DISPOSED;
        }


        synchronized int getSharedCount() {
            return this.sharedCount;
        }


        synchronized S createSharedObject() {
            ensureActive();
            this.sharedCount++;

            return LockingSharedObjectPool.this.createSharedObject(this.pooledObject, this::disposeSharedObject);
        }


        private void disposeSharedObject() {
            // Should we offer the pool to dispose of this entry after the synchronized block?
            boolean offerDisposeEntry = false;

            synchronized (this) {
                ensureActive();

                if (this.sharedCount == 0) {
                    // TODO: log notification that the number of disposes does not match number of acquires.
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
            }

            if (offerDisposeEntry) {
                LockingSharedObjectPool.this.offerDisposeEntry(this);
            }
        }


        private void ensureNew() {
            assert (Thread.holdsLock(this));

            if (this.sharedCount != NEW) {
                throw new IllegalStateException("Entry is already initialized");
            }
        }


        private void ensureActive() {
            assert (Thread.holdsLock(this));

            if (this.sharedCount == NEW) {
                throw new IllegalStateException("Entry is not initialized yet");
            }
            if (this.sharedCount == DISPOSED) {
                throw new IllegalStateException("Entry is already disposed of");
            }
        }
    }
}