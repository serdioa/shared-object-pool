package de.serdioa.common.pool.jmh;

import java.util.Objects;


/**
 * A hand-crafted implementation of the {@link SharedTestObject} using synchronization.
 */
public class SynchronizedSharedTestObject implements SharedTestObject {

    // @GuardedBy(this.mutex)
    private TestObject pooled;

    private final Object mutex = new Object();


    public SynchronizedSharedTestObject(TestObject pooled) {
        synchronized (this.mutex) {
            this.pooled = Objects.requireNonNull(pooled);
        }
    }


    @Override
    public void run(int tokens) {
        synchronized (this.mutex) {
            if (this.pooled == null) {
                throw new IllegalStateException("Shared object is already disposed of");
            } else {
                this.pooled.run(tokens);
            }
        }
    }


    @Override
    public void dispose() {
        synchronized (this.mutex) {
            if (this.pooled == null) {
                throw new IllegalStateException("Shared object is already disposed of");
            } else {
                this.pooled = null;
            }
        }
    }


    @Override
    public boolean isDisposed() {
        synchronized (this.mutex) {
            return (this.pooled == null);
        }
    }
}
