package de.serdioa.common.pool.sample;

import de.serdioa.common.pool.SharedObject;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Hand-written implementation of the {@link SharedObject} for {@link Counter} based on synchronization.
 */
public class SynchronizedSharedCounter implements SharedCounter {

    private static final Logger logger = LoggerFactory.getLogger(SynchronizedSharedCounter.class);

    private final String key;
    private final PooledCounter pooledCounter;
    private final Runnable disposeCallback;

    private volatile boolean dummy = false;

    // @GuardedBy(mutex)
    private boolean disposed = false;

    private final Object mutex = new Object();


    public SynchronizedSharedCounter(PooledCounter pooledCounter, Runnable disposeCallback) {
        this.pooledCounter = Objects.requireNonNull(pooledCounter);
        this.disposeCallback = Objects.requireNonNull(disposeCallback);
        this.key = this.pooledCounter.getKey();

        logger.trace("Constructed SharedCounter [{}]", this.key);
    }


    @Override
    public void dispose() {
        synchronized (this.mutex) {
            if (!this.disposed) {
                logger.trace("Disposing of SharedCounter[{}]", this.key);
                this.disposed = true;
                this.disposeCallback.run();

                // This code actually never executes, but since dummy is volatile, JVM can't optimize it away
                // and can't GC this object before the disposeCallback() above is finished.
                if (dummy) {
                    this.get();
                }
            } else {
                logger.trace("Skipped disposing of SharedCounter[{}] - already disposed", this.key);
            }
        }
    }


    @Override
    public boolean isDisposed() {
        synchronized (this.mutex) {
            return this.disposed;
        }
    }


    @Override
    public int get() {
        synchronized (this.mutex) {
            ensureActive();
            return this.pooledCounter.get();
        }
    }


    @Override
    public int increment() {
        synchronized (this.mutex) {
            ensureActive();
            return this.pooledCounter.increment();
        }
    }


    @Override
    public int decrement() {
        synchronized (this.mutex) {
            ensureActive();
            return this.pooledCounter.decrement();
        }
    }


    private void ensureActive() {
        if (this.disposed) {
            throw new IllegalStateException("SharedCounter[" + this.key + "] is already disposed of");
        }
    }
}
