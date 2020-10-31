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
    };

    private final String key;

    // The pooled object backing this shared object.
    // A null pooled object indicates that this shared object has been disposed of.
    // @GuardedBy(lock)
    private PooledCounter pooledCounter;

    // The callback to be invoked when a client disposes of this shared object.
    private final Runnable disposeCallback;

    private volatile boolean dummy = false;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();


    public LockingSharedCounter(PooledCounter pooledCounter, Runnable disposeCallback) {
        Lock readLock = this.lock.readLock();
        readLock.lock();
        try {
            this.pooledCounter = Objects.requireNonNull(pooledCounter);
            this.disposeCallback = Objects.requireNonNull(disposeCallback);
            this.key = this.pooledCounter.getKey();
        } finally {
            readLock.unlock();
        }

        logger.trace("Constructed SharedCounter [{}]", this.key);
    }


    @Override
    public void dispose() {
        Lock writeLock = this.lock.writeLock();
        writeLock.lock();
        try {
            if (this.pooledCounter == null) {
                // This shared object already has been disposed of.
                throw new IllegalStateException("Method dispose() called on already disposed SharedCounter["
                        + this.key + "]");
            }

            // Mark this shared object as disposed.
            this.pooledCounter = null;
        } finally {
            writeLock.unlock();
        }

        // Invoke the dispose callback outside of the locked block.
        // If we would invoke the callback in the locked block, we may get a deadlock when a pool and a client attempt
        // to dispose of the shared object simultaneously.
        this.disposeCallback.run();

        // This code actually never executes, but since dummy is volatile, JVM can't optimize it away
        // and can't GC this object before the disposeCallback() above is finished.
        // Without this call in seldom cases I have observed the following behaviour:
        //
        // * The client calls dispose() on this shared object.
        // * This shared object calls disposeCallback, informing the pool.
        // * Before the method disposeCallback executes, GC detects that this shared object is not used anymore and
        // claims it.
        // * The reaper thread in the pool which tracks shared objects which were GC'ed without being properly disposed
        // of finds out that this shared object has been GC'ed. Since the disposeCallback is not executed yet,
        // the reaper decides that this object had been GC'ed without being disposed of, and writes a warning.
        // * The method disposeCallback finishes, but a false positive warning is already in the log.
        //
        // To put it short, no error happens, but without the method call below there are false positive log messages
        // about shared objects not properly disposed of.
        if (dummy) {
            this.get();
        }
    }


    @Override
    public boolean isDisposed() {
        Lock readLock = this.lock.readLock();
        readLock.lock();
        try {
            return (this.pooledCounter == null);
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
        if (this.pooledCounter == null) {
            throw new IllegalStateException("SharedCounter[" + this.key + "] is already disposed of");
        }
    }


    public static SharedObjectFactory<PooledCounter, SharedCounter> factory() {
        return LockingSharedCounter.FACTORY;
    }
}
