package de.serdioa.common.pool;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


// Simple implementation using synchronization.
public class SynchronizedSharedObjectPool<K, S extends SharedObject, P> extends AbstractSharedObjectPool<K, S, P> {

    private static final Logger logger = LoggerFactory.getLogger(SynchronizedSharedObjectPool.class);

    // Pooled entries.
    // @GuardedBy(lock)
    private final Map<K, Entry> entries = new HashMap<>();

    // Duration in milliseconds to keep idle pooled objects before disposing of them. Non-positive number means
    // disposing of idle pooled objects immediately.
    // If initializing a pooled object is an expensive operation, for example if it requires loading large amount
    // of data from a remote source, and the number of pooled objects is not too high, it could make sense to keep
    // idle pooled objects for some time, to prevent an expensive initialization of a pooled object will become required
    // shortly afterwards.
    private final long idleDisposeTimeMillis;

    // The executor service for running disposals.
    private final ScheduledExecutorService disposeExecutor;

    // Synchronization monitor.
    private final Object lock = new Object();


    private SynchronizedSharedObjectPool(String name,
            PooledObjectFactory<K, P> pooledObjectFactory,
            SharedObjectFactory<P, S> sharedObjectFactory,
            EvictionPolicy evictionPolicy,
            long idleDisposeTimeMillis,
            int disposeThreads) {
        super(name, pooledObjectFactory, sharedObjectFactory, evictionPolicy);

        this.idleDisposeTimeMillis = idleDisposeTimeMillis;

        // Create an executor for disposals only if asynchronous disposal is configured.
        if (this.idleDisposeTimeMillis > 0) {
            ThreadFactory disposerThreadFactory = new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger();


                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, SynchronizedSharedObjectPool.this.name + "-" + counter.getAndDecrement());
                }
            };
            this.disposeExecutor = Executors.newScheduledThreadPool(disposeThreads, disposerThreadFactory);
        } else {
            // Synchronous disposal is configured, no executor is required.
            this.disposeExecutor = null;
        }
    }


    @Override
    public void dispose() {
        super.dispose();

        // If a disposal executor exist (that is, if an asynchronous disposal was configured), stop the executor
        // without waiting for disposal tasks.
        if (this.disposeExecutor != null) {
            this.disposeExecutor.shutdownNow();
        }
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
                } else if (entrySharedCount == Entry.NEW) {
                    // The entry was just added to the pool either by this thread or by another thread.
                    // Since this thread synchronized on the entry first, we have to initialize it.
                    try {
                        entry.initialize();
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
                synchronized (this.lock) {
                    this.entries.remove(key, entry);
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
        synchronized (this.lock) {
            Entry entry = this.entries.get(key);
            if (entry == null) {
                entry = createEntry(key);
                this.entries.put(key, entry);
            }

            return entry;
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
        logger.trace("!!! Offered to dispose of entry {}", entry.getKey());

        // Should we remove entry from the cache after the synchronized block?
        boolean removeEntryFromCache;

        synchronized (entry) {
            // An entry may be disposed of only if it is not providing any shared objects. Since this method
            // is called without holding a lock on entry, it could be that in the meantime another thread acquired
            // a new shared object from the entry.
            int entrySharedCount = entry.getSharedCount();
            if (entrySharedCount > 0) {
                // The entry is providing some shared objects, so it can't be disposed of.
                logger
                        .trace("!!! The entry {} provides {} shared objects, skipping disposal", entry.getKey(), entrySharedCount);
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
                logger.trace("!!! The entry {} is already disposed of, skipping disposal", entry.getKey());
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

                logger.trace("!!! Entry {}: lastReturnTime={}, disposeAt={}, now={}, delayBeforeDispose={}",
                        entry.getKey(), lastReturnTime, disposeAt, now, delayBeforeDispose);

                if (delayBeforeDispose > 0) {
                    // Instead of disposing of the entry immediately, schedule to try again later.
                    ScheduledFuture<?> disposeTask = this.disposeExecutor.schedule(() -> this.offerDispose(entry),
                            delayBeforeDispose, TimeUnit.MILLISECONDS);
                    entry.setDisposeTask(disposeTask);

                    logger.trace("!!! Entry {}: scheduled disposal after {} ms", entry.getKey(), delayBeforeDispose);

                    // We have scheduled a later attempt.
                    disposeEntryImmediately = false;
                } else {
                    // Generally asynchronous disposal of entries is configured, but for this entry the idle time
                    // already expired and we shall dispose of it immediately.
                    logger.trace("!!! Entry {}: delayBeforeDispose <= 0, disposing immediately", entry.getKey());
                    disposeEntryImmediately = true;
                }
            } else {
                // A synchronous disposal of entries is configured, we never wait.
                logger.trace("!!! Entry {}: synchronous disposal configured, diposing immediately", entry.getKey());
                disposeEntryImmediately = true;
            }

            // Dispose of the entry. Note that the entry still remains in the cache.
            if (disposeEntryImmediately) {
                logger.trace("!!! Entry {}: disposing", entry.getKey());

                try {
                    entry.dispose();
                } catch (Exception ex) {
                    logger.error("Exception when disposing of entry {}", entry.getKey(), ex);
                }

                logger.trace("!!! Entry {}: disposed", entry.getKey());

                // We have disposed of the entry, so we shall remove it from cache after the synchronized block.
                removeEntryFromCache = true;
            } else {
                // We have not disposed of the entry: either it is not eligible for the disposal at all (for example,
                // because it provides shared objects), or we have scheduled the entry for a later disposal.
                // Either way, the entry shall remain in the cache.
                removeEntryFromCache = false;
            }
        }

        // Remove the entry from the cache, if it has been disposed of. When removing, make sure that we are removing
        // the right entry to prevent case when another thread had already removed the "bad" entry and inserted into
        // the cache another, "good" one.
        if (removeEntryFromCache) {
            logger.trace("!!! Entry {}: removing from cache", entry.getKey());

            synchronized (this.lock) {
                this.entries.remove(entry.getKey(), entry);
            }

            logger.trace("!!! Entry {}: removed from cache", entry.getKey());
        }
    }


    public int getPooledObjectsCount() {
        synchronized (this.lock) {
            return this.entries.size();
        }
    }


    public int getSharedObjectsCount(K key) {
        synchronized (this.lock) {
            Entry entry = this.entries.get(key);
            return (entry == null ? 0 : entry.getSharedCount());
        }
    }


    public boolean containsPooledObject(K key) {
        synchronized (this.lock) {
            return this.entries.containsKey(key);
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

        // Last time the last shared object from this entry was returned. The time makes sense only if this entry
        // currently does not provide any shared objects, that is if sharedCount == 0, because the time is NOT reset
        // when new shared objects are created.
        // @GuardedBy(this)
        private long lastReturnTime = 0;

        // The ScheduledFuture for asynchronously disposing of this entry, if any.
        // A ScheduledFuture is set when this entry is scheduled for an asynchronous disposal.
        // If this entry provides a new shared object before being disposed, it may cancel the ScheduledFuture.
        // Even if the ScheduledFuture is not cancellled, it will not dispose of this entry when executed, if the
        // entry provides a shared object.
        // @GuardedBy(this)
        private ScheduledFuture<?> disposeTask;


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


        synchronized void initialize() throws InitializationException {
            logger.trace("!!! Inside entry {}: initializing", this.key);

            ensureNew();

            try {
                SynchronizedSharedObjectPool.this.initializePooledObject(this.pooledObject);
                this.sharedCount = 0;
            } catch (Exception ex) {
                // An attempt to initialize this entry failed. Mark the entry as disposed and re-throw the exception.
                this.sharedCount = DISPOSED;
                throw InitializationException.wrap(this.key, ex);
            }

            logger.trace("!!! Inside entry {}: initialized", this.key);
        }


        synchronized void dispose() {
            logger.trace("!!! Inside entry {}: disposing", this.key);

            ensureActive();

            SynchronizedSharedObjectPool.this.disposePooledObject(this.pooledObject);
            this.sharedCount = DISPOSED;

            logger.trace("!!! Inside entry {}: disposed", this.key);
        }


        synchronized int getSharedCount() {
            return this.sharedCount;
        }


        synchronized long getLastReturnTime() {
            return this.lastReturnTime;
        }


        synchronized void setDisposeTask(ScheduledFuture<?> disposeTask) {
            this.disposeTask = disposeTask;
        }


        synchronized S createSharedObject() {
            logger.trace("!!! Inside entry {}: creating shared object", this.key);

            ensureActive();

            S sharedObject = SynchronizedSharedObjectPool.this
                    .createSharedObject(this.pooledObject, this::disposeSharedObject);
            this.sharedCount++;

            logger.trace("!!! Inside entry {}: created shared object, sharedCount={}", this.key, this.sharedCount);

            // If this entry was scheduled for disposal, attempt to cancel the dispose task.
            // This entry will not be disposed of even if the task can not be cancelled (the task will not dispose
            // of this entry if it provides any shared objects), but canelling the task reduces unnecessary load
            // on the disposal executor.
            if (this.disposeTask != null) {
                this.disposeTask.cancel(true);
                this.disposeTask = null;
                logger.trace("!!! Inside entry {}: cancelled dispose task", this.key);
            }

            return sharedObject;
        }


        private void disposeSharedObject() {
            logger.trace("!!! Inside entry {}: disposing of shared object", this.key);

            // Should we offer the pool to dispose of this entry after the synchronized block?
            boolean offerDisposeEntry = false;

            synchronized (this) {
                ensureActive();

                if (this.sharedCount == 0) {
                    logger.warn("Disposed of a shared object {}, but shared object counter is 0", this.key);
                } else {
                    this.sharedCount--;
                }

                logger
                        .trace("!!! Inside entry {}: disposed of shared object, sharedCount={}", this.key, this.sharedCount);

                if (this.sharedCount == 0) {
                    // This entry is not providing any shared objects anymore, and may be disposed of.
                    // It is up to the pool to deside if the entry should actually be disposed of.
                    // A pool implementation may decide to dispose of the entry immediately, or it may decide to keep
                    // an entry active for a time, so that it is immediately available if required.
                    offerDisposeEntry = true;

                    // Set the time when the last shared object was returned.
                    this.lastReturnTime = System.currentTimeMillis();

                    logger.trace("!!! Inside entry {}: disposed of last shared object, will offer dispose", this.key);
                }
            }

            if (offerDisposeEntry) {
                logger.trace("!!! Inside entry {}: offer dispose", this.key);
                SynchronizedSharedObjectPool.this.offerDispose(this);
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


    public static class Builder<K, S extends SharedObject, P> {

        // An optional name of the pool. The name is used for log messages and in names of background threads.
        private String name;

        // Factory for creating new pooled objects.
        private PooledObjectFactory<K, P> pooledObjectFactory;

        // Factory for creating shared objects from pooled objects.
        private SharedObjectFactory<P, S> sharedObjectFactory;

        // The policy for evicting non-used pooled objects.
        private EvictionPolicy evictionPolicy;

        // Duration in milliseconds to keep idle pooled objects before disposing of them. Non-positive number means
        // disposing of idle pooled objects immediately.
        // By default idle pooled objects are disposed of immediately.
        private long idleDisposeTimeMillis;

        // The number of threads asynchronously disposing of idle objects.
        // By default idle pooled objects are disposed of immediately, so no threads for asynchronous disposal
        // are configured.
        int disposeThreads;


        public Builder<K, S, P> setName(String name) {
            this.name = name;
            return this;
        }


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


        public Builder<K, S, P> setIdleDisposeTimeMillis(long idleDisposeTimeMillis) {
            this.idleDisposeTimeMillis = idleDisposeTimeMillis;
            return this;
        }


        public Builder<K, S, P> setDisposeThreads(int disposeThreads) {
            this.disposeThreads = disposeThreads;
            return this;
        }


        public SynchronizedSharedObjectPool<K, S, P> build() {
            // The name is optional.
            if (this.pooledObjectFactory == null) {
                throw new IllegalStateException("pooledObjectFactory is required");
            }
            if (this.sharedObjectFactory == null) {
                throw new IllegalStateException("sharedObjectFactory is required");
            }
            if (this.evictionPolicy == null) {
                throw new IllegalStateException("evictionPolicy is required");
            }
            // A non-positive idleDisposeTimeMillis is valid, it indicates that idle objects shall be disposed of
            // immediately.

            // If idleDisposeTimeMillis > 0, that is a postponed disposal is requested, the number of dispose threads
            // must be > 0.
            if (this.idleDisposeTimeMillis > 0 && this.disposeThreads <= 0) {
                throw new IllegalStateException("idleDisposeTimeMillis (" + this.idleDisposeTimeMillis + ") > 0, "
                        + "but disposeThreads (" + this.disposeThreads + ") <= 0");
            }

            return new SynchronizedSharedObjectPool<>(this.name, this.pooledObjectFactory, this.sharedObjectFactory,
                    this.evictionPolicy, this.idleDisposeTimeMillis, this.disposeThreads);
        }
    }
}
