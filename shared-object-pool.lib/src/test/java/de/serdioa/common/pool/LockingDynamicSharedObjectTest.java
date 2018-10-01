package de.serdioa.common.pool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import de.serdioa.common.pool.sample.Counter;
import de.serdioa.common.pool.sample.PooledCounter;
import org.junit.Before;
import org.junit.Test;


public class LockingDynamicSharedObjectTest {

    private interface SharedCounter extends Counter, SharedObject {}

    private PooledCounter pooledCounter;
    private SharedCounter sharedCounter;

    // Number of times the method dispose() was called on a shared counter.
    private int disposeCalled = 0;

    @Before
    public void setUp() {
        this.pooledCounter = new PooledCounter("AAA");
        this.pooledCounter.init();

        this.sharedCounter = LockingDynamicSharedObject.create(SharedCounter.class, this.pooledCounter, () -> {
            this.disposeCalled++;
        });
    }


    @Test
    public void testGet() {
        assertEquals(0, this.sharedCounter.get());
    }


    @Test
    public void testIncrement() {
        assertEquals(1, this.sharedCounter.increment());
    }


    @Test
    public void testDecrement() {
        assertEquals(-1, this.sharedCounter.decrement());
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
