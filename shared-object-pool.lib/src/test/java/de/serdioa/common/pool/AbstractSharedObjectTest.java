package de.serdioa.common.pool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import de.serdioa.common.pool.sample.PooledCounter;
import de.serdioa.common.pool.sample.SharedCounter;
import org.junit.Before;
import org.junit.Test;


/**
 * Base class for unit tests for reflection-based implementation of a {@link SharedObject}.
 */
public abstract class AbstractSharedObjectTest {

    protected SharedObjectFactory<PooledCounter, SharedCounter> factory;
    protected PooledCounter pooledCounter;
    protected SharedCounter sharedCounter;

    // Number of times the method dispose() was called on a shared counter.
    private int disposeCalled = 0;


    @Before
    public void setUp() {
        this.factory = sharedObjectFactory();

        this.pooledCounter = new PooledCounter("AAA");
        this.pooledCounter.initialize();

        this.sharedCounter = this.factory.createShared(this.pooledCounter, () -> {
            this.disposeCalled++;
        });
    }


    protected abstract SharedObjectFactory<PooledCounter, SharedCounter> sharedObjectFactory();


    @Test
    public void testGet() {
        assertEquals(0, this.sharedCounter.get());
    }


    @Test
    public void testIncrement() {
        assertEquals(1, this.sharedCounter.increment());
        assertEquals(1, this.sharedCounter.get());
    }


    @Test
    public void testDecrement() {
        assertEquals(-1, this.sharedCounter.decrement());
        assertEquals(-1, this.sharedCounter.get());
    }


    @Test
    public void testDispose() {
        this.sharedCounter.dispose();
        assertEquals(1, this.disposeCalled);
    }


    @Test(expected = IllegalStateException.class)
    public void testRepeatedDispose() {
        this.sharedCounter.dispose();
        this.sharedCounter.dispose();
    }


    @Test(expected = IllegalStateException.class)
    public void testMethodCallAfterDispose() {
        this.sharedCounter.dispose();
        this.sharedCounter.get();
    }


    @Test
    public void testIsDisposed_notDisposed() {
        assertFalse(this.sharedCounter.isDisposed());
    }


    @Test
    public void testIsDisposed_disposed() {
        this.sharedCounter.dispose();
        assertTrue(this.sharedCounter.isDisposed());
    }
}
