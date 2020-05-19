package de.serdioa.common.pool.sample;

import de.serdioa.common.pool.SharedObject;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import de.serdioa.common.pool.SharedObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Hand-written implementation of the {@link SharedObject} for {@link Counter} based on locks.
 */
public class LockingSharedCounter implements SharedCounter {

    private static final Logger logger = LoggerFactory.getLogger(LockingSharedCounter.class);

    private static final SharedObjectFactory<PooledCounter, SharedCounter> FACTORY =
            new SharedObjectFactory<PooledCounter, SharedCounter>() {
        @Override
        public SharedCounter createShared(PooledCounter pooledObject, Runnable disposeCallback) {
            return new LockingSharedCounter(pooledObject, disposeCallback);
        }


        @Override
        public void disposeByPool(SharedCounter sharedObject) {
            ((LockingSharedCounter) sharedObject).disposeByPool();
        }
    };

    private final String key;
    private final PooledCounter pooledCounter;
    private final Runnable disposeCallback;

    private volatile boolean dummy = false;

    // @GuardedBy(lock)
    private boolean disposed = false;
    private boolean disposedByPool;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();


    public LockingSharedCounter(PooledCounter pooledCounter, Runnable disposeCallback) {
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
            if (this.disposed) {
                // This shared object already has been disposed of.
                if (this.disposedByPool) {
                    // If it has been disposed by the pool when the complete pool has been disposed, and now a client
                    // attemps to dispose of the object again, just return: we have nothing to do (this shared object
                    // already has been disposed of), and it is not an error, but a possible normal case during shutdown
                    // of the application.
                    return;
                } else {
                    // On the other hand, if this shared object has been disposed of by a client, and now a client
                    // attempts to dispose of this shared object again, it indicates an error.
                    throw new IllegalStateException("Method dispose() called on already disposed SharedCounter["
                            + this.key + "]");
                }
            }

            logger.trace("Disposing of SharedCounter[{}]", this.key);
            this.disposed = true;
            this.disposeCallback.run();

            // This code actually never executes, but since dummy is volatile, JVM can't optimize it away
            // and can't GC this object before the disposeCallback() above is finished.
            if (dummy) {
                this.get();
            }
        } finally {
            writeLock.unlock();
        }
    }


    private void disposeByPool() {
        Lock writeLock = this.lock.writeLock();
        writeLock.lock();
        try {
            if (this.disposed) {
                // This shared object already has been disposed of.
                if (this.disposedByPool) {
                    throw new IllegalStateException("Pool attempts to dispose of a SharedCounter[" + this.key
                            + "] already disposed of by the pool");
                } else {
                    throw new IllegalStateException("Pool attempts to dispose of an already disposed SharedCounter ["
                            + this.key + "]");
                }
            }

            // When disposing by the pool, do not call the dispose callback: the pool disposes of the shared object
            // itself, and we do not need to notify it back.
            //
            // Mark this invocation handler as disposed, and allow to GC the shared object proxy.
            this.disposed = true;
            this.disposedByPool = true;
        } finally {
            writeLock.unlock();
        }
    }


    @Override
    public boolean isDisposed() {
        Lock readLock = this.lock.readLock();
        readLock.lock();
        try {
            return this.disposed;
        } finally {
            readLock.unlock();
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
            if (this.disposedByPool) {
                throw new IllegalStateException("SharedCounter[" + this.key + "] is already disposed of by the pool");
            } else {
                throw new IllegalStateException("SharedCounter[" + this.key + "] is already disposed of");
            }
        }
    }


    public static SharedObjectFactory<PooledCounter, SharedCounter> factory() {
        return LockingSharedCounter.FACTORY;
    }
}
