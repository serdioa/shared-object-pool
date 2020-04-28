package de.serdioa.common.pool;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public abstract class AbstractSharedObjectPool<K, S extends SharedObject, P> implements SharedObjectPool<K, S> {

    // A static counter used to create unique names for object pools.
    private static final AtomicInteger NAME_COUNTER = new AtomicInteger();

    // Name of this object pool used for logging and naming threads.
    protected final String name;

    // Factory for creating new pooled objects.
    private final PooledObjectFactory<K, P> pooledObjectFactory;

    // Factory for creating shared objects from pooled objects.
    private final SharedObjectFactory<P, S> sharedObjectFactory;

    // Duration in milliseconds to keep idle pooled objects before disposing of them. Non-positive number means
    // disposing of idle pooled objects immediately.
    // If initializing a pooled object is an expensive operation, for example if it requires loading large amount
    // of data from a remote source, and the number of pooled objects is not too high, it could make sense to keep
    // idle pooled objects for some time, to prevent an expensive initialization of a pooled object will become required
    // shortly afterwards.
    protected final long idleDisposeTimeMillis;

    // The executor service for running disposals.
    private final ScheduledExecutorService disposeExecutor;


    protected AbstractSharedObjectPool(final String name,
            PooledObjectFactory<K, P> pooledObjectFactory,
            SharedObjectFactory<P, S> sharedObjectFactory,
            long idleDisposeTimeMillis,
            int disposeThreads) {

        this.name = (name != null ? null : this.getClass().getSimpleName() + '-' + NAME_COUNTER.getAndIncrement());
        this.pooledObjectFactory = Objects.requireNonNull(pooledObjectFactory);
        this.sharedObjectFactory = Objects.requireNonNull(sharedObjectFactory);

        this.idleDisposeTimeMillis = idleDisposeTimeMillis;
        this.disposeExecutor = this.buildDisposeExecutor(disposeThreads);
    }


    private ScheduledExecutorService buildDisposeExecutor(int disposeThreads) {
        if (this.idleDisposeTimeMillis > 0) {
            if (disposeThreads <= 0) {
                throw new IllegalArgumentException("idleDisposeTimeMillis (" + this.idleDisposeTimeMillis + " > 0, "
                        + "but disposeThreads (" + disposeThreads + ") <= 0");
            }

            ThreadFactory threadFactory = new DisposeExecutorThreadFactory(this.name);
            return Executors.newScheduledThreadPool(disposeThreads, threadFactory);
        } else {
            // Synchronous disposal is configured, no executor is required.
            return null;
        }
    }


    public String getName() {
        return this.name;
    }


    public void dispose() {
        // If a disposal executor exist (that is, if an asynchronous disposal was configured), stop the executor
        // without waiting for disposal tasks.
        if (this.disposeExecutor != null) {
            this.disposeExecutor.shutdownNow();
        }
    }


    protected P createPooledObject(K key) throws InvalidKeyException {
        return this.pooledObjectFactory.create(key);
    }


    protected void initializePooledObject(P pooledObject) {
        this.pooledObjectFactory.initialize(pooledObject);
    }


    protected void disposePooledObject(P pooledObject) {
        this.pooledObjectFactory.dispose(pooledObject);
    }


    protected S createSharedObject(P pooledObject, Runnable disposeCallback) {
        return this.sharedObjectFactory.createShared(pooledObject, disposeCallback);
    }


    protected ScheduledFuture<?> scheduleDisposeTask(Runnable command, long delay, TimeUnit unit) {
        return this.disposeExecutor.schedule(command, delay, unit);
    }


    private static final class DisposeExecutorThreadFactory implements ThreadFactory {

        private final String name;
        private final AtomicInteger counter = new AtomicInteger();


        public DisposeExecutorThreadFactory(String name) {
            this.name = Objects.requireNonNull(name);
        }


        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, this.name + "-disposer-" + counter.getAndIncrement());
        }

    }
}
