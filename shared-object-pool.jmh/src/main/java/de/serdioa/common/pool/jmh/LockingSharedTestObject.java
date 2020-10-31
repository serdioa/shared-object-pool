package de.serdioa.common.pool.jmh;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * A hand-crafted implementation of the {@link SharedTestObject} using locks.
 */
public class LockingSharedTestObject implements SharedTestObject {

    // @GuardedBy(this.lock)
    private TestObject pooled;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();


    public LockingSharedTestObject(TestObject pooled) {
        final Lock exclusiveLock = this.lock.readLock();
        exclusiveLock.lock();
        try {
            this.pooled = Objects.requireNonNull(pooled);
        } finally {
            exclusiveLock.unlock();
        }
    }


    @Override
    public void run(int tokens) {
        final Lock sharedLock = this.lock.readLock();
        sharedLock.lock();
        try {
            if (this.pooled == null) {
                throw new IllegalStateException("Shared object is already disposed of");
            } else {
                this.pooled.run(tokens);
            }
        } finally {
            sharedLock.unlock();
        }
    }


    @Override
    public void dispose() {
        final Lock exclusiveLock = this.lock.writeLock();
        exclusiveLock.lock();
        try {
            if (this.pooled == null) {
                throw new IllegalStateException("Shared object is already disposed of");
            } else {
                this.pooled = null;
            }
        } finally {
            exclusiveLock.unlock();
        }
    }


    @Override
    public boolean isDisposed() {
        final Lock sharedLock = this.lock.readLock();
        sharedLock.lock();
        try {
            return (this.pooled == null);
        } finally {
            sharedLock.unlock();
        }
    }
}
