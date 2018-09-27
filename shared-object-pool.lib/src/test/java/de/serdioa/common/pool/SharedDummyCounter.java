package de.serdioa.common.pool;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SharedDummyCounter implements DummyCounter, SharedObject {
    private static final Logger logger = LoggerFactory.getLogger(SharedDummyCounter.class);

    private final String key;
    private final PooledDummyCounter pooledCounter;
    private final Runnable disposeCallback;

    // @GuardedBy(lock)
    private boolean disposed = false;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    SharedDummyCounter(PooledDummyCounter pooledCounter, Runnable disposeCallback) {
        this.pooledCounter = Objects.requireNonNull(pooledCounter);
        this.disposeCallback = Objects.requireNonNull(disposeCallback);
        this.key = this.pooledCounter.getKey();

        logger.trace("Constructed SharedDummyCounter [{}]", this.key);
    }

    @Override
    public void dispose() {
        Lock writeLock = this.lock.writeLock();
        writeLock.lock();
        try {
            if (!this.disposed) {
                logger.trace("Disposing of SharedDummyCounter[{}]", this.key);
                this.disposed = true;
                this.disposeCallback.run();
            } else {
                logger.trace("Skipped disposing of SharedDummyCounter[{}] - already disposed", this.key);
            }
        } finally {
            writeLock.unlock();
        }
    }


    @Override
    public int get() {
        Lock readLock = this.lock.readLock();
        readLock.lock();
        try {
            ensureActive();
            return this.pooledCounter.get();
        } finally {
            readLock.unlock();
        }
    }


    @Override
    public int increment() {
        Lock readLock = this.lock.readLock();
        readLock.lock();
        try {
            ensureActive();
            return this.pooledCounter.increment();
        } finally {
            readLock.unlock();
        }
    }


    @Override
    public int decrement() {
        Lock readLock = this.lock.readLock();
        readLock.lock();
        try {
            ensureActive();
            return this.pooledCounter.decrement();
        } finally {
            readLock.unlock();
        }
    }


    private void ensureActive() {
        if (this.disposed) {
            throw new IllegalStateException("SharedDummyCounter[" + this.key + "] is already disposed of");
        }
    }
}
