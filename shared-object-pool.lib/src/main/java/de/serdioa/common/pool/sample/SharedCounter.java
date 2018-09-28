package de.serdioa.common.pool.sample;

import de.serdioa.common.pool.SharedObject;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SharedCounter implements Counter, SharedObject {
    private static final Logger logger = LoggerFactory.getLogger(SharedCounter.class);

    private final String key;
    private final PooledCounter pooledCounter;
    private final Runnable disposeCallback;

    // @GuardedBy(lock)
    private boolean disposed = false;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    SharedCounter(PooledCounter pooledCounter, Runnable disposeCallback) {
        this.pooledCounter = Objects.requireNonNull(pooledCounter);
        this.disposeCallback = Objects.requireNonNull(disposeCallback);
        this.key = this.pooledCounter.getKey();

        logger.trace("Constructed SharedCounter [{}]", this.key);
    }

    @Override
    public void dispose() {
        Lock writeLock = this.lock.writeLock();
        writeLock.lock();
        try {
            if (!this.disposed) {
                logger.trace("Disposing of SharedCounter[{}]", this.key);
                this.disposed = true;
                this.disposeCallback.run();
            } else {
                logger.trace("Skipped disposing of SharedCounter[{}] - already disposed", this.key);
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
            throw new IllegalStateException("SharedCounter[" + this.key + "] is already disposed of");
        }
    }
}
