package de.serdioa.common.pool.sample;

import de.serdioa.common.pool.InitializationException;
import de.serdioa.common.pool.PooledObject;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PooledCounter implements Counter, PooledObject {
    private static final Logger logger = LoggerFactory.getLogger(PooledCounter.class);

    private enum Status {
        NEW, ACTIVE, DISPOSED;
    }

    private final String key;
    private final AtomicInteger counter = new AtomicInteger();

    // @GuardedBy(lock)
    private Status status = Status.NEW;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public PooledCounter(String key) {
        this.key = Objects.requireNonNull(key);
        logger.trace("Constructed PooledCounter[{}]", this.key);
    }


    @Override
    public void init() throws InitializationException {
        Lock writeLock = this.lock.writeLock();
        writeLock.lock();
        try {
            logger.trace("Initializing PooledCounter[{}]", this.key);
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
            logger.trace("Disposing of PooledCounter[{}]", this.key);
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
            throw new IllegalStateException("PooledCounter[" + this.key + "] is already disposed of");
        }
    }
}
