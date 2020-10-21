package de.serdioa.common.pool.jmh;

import java.util.Objects;


/**
 * A hand-crafted implementation of the {@link SharedTestObject} using synchronization.
 */
public class SynchronizedSharedTestObject implements SharedTestObject {

    private final TestObject pooled;

    // @GuardedBy(mutex)
    private boolean disposed = false;

    private final Object mutex = new Object();


    public SynchronizedSharedTestObject(TestObject pooled) {
        this.pooled = Objects.requireNonNull(pooled);
    }


    @Override
    public void run(int tokens) {
        synchronized (this.mutex) {
            if (this.disposed) {
                throw new IllegalStateException("Shared object is already disposed of");
            } else {
                this.pooled.run(tokens);
            }
        }
    }


    @Override
    public void dispose() {
        synchronized (this.mutex) {
            if (this.disposed) {
                throw new IllegalStateException("Shared object is already disposed of");
            } else {
                this.disposed = true;
            }
        }
    }


    @Override
    public boolean isDisposed() {
        synchronized (this.mutex) {
            return this.disposed;
        }
    }
}
