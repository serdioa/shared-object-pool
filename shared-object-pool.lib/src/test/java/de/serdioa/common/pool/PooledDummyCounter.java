package de.serdioa.common.pool;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PooledDummyCounter implements DummyCounter, PooledObject {
    private static final Logger logger = LoggerFactory.getLogger(PooledDummyCounter.class);

    private enum Status {
        NEW, ACTIVE, DISPOSED;
    }

    private final String key;
    private AtomicInteger counter = new AtomicInteger();

    // @GuardedBy(lock)
    private Status status = Status.NEW;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public PooledDummyCounter(String key) {
        this.key = Objects.requireNonNull(key);
        logger.trace("Constructed PooledDummyCounter[{}]", this.key);
    }


    @Override
    public void init() throws InitializationException {
        Lock writeLock = this.lock.writeLock();
        writeLock.lock();
        try {
            logger.trace("Initializing PooledDummyCounter[{}]", this.key);
            if (this.status == Status.NEW) {
                this.status = Status.ACTIVE;
            } else {
                throw new InitializationException("status is " + this.status);
            }
        } finally {
            writeLock.unlock();
        }
    }


    @Override
    public void dispose() {
        Lock writeLock = this.lock.writeLock();
        writeLock.lock();
        try {
            logger.trace("Disposing of PooledDummyCounter[{}]", this.key);
            if (this.status == Status.ACTIVE) {
                this.status = Status.DISPOSED;
            } else {
                throw new InitializationException("status is " + this.status);
            }
        } finally {
            writeLock.unlock();
        }
    }


    public String getKey() {
        return this.key;
    }


    @Override
    public int get() {
        Lock readLock = this.lock.readLock();
        readLock.lock();
        try {
            ensureActive();
            return this.counter.get();
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
            return this.counter.incrementAndGet();
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
            return this.counter.decrementAndGet();
        } finally {
            readLock.unlock();
        }
    }


    private void ensureActive() {
        if (this.status != Status.ACTIVE) {
            throw new IllegalStateException("PooledDummyCounter[" + this.key + "] is already disposed of");
        }
    }
}
