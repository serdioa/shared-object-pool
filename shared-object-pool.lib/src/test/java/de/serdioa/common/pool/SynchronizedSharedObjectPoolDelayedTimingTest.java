package de.serdioa.common.pool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import de.serdioa.common.pool.sample.PooledCounter;
import de.serdioa.common.pool.sample.PooledCounterFactory;
import de.serdioa.common.pool.sample.SharedCounter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class SynchronizedSharedObjectPoolDelayedTimingTest {

    private static final long TICK = 100;
    private static final long TICK_OVER_2 = 50;
    private static final long TICK_2_OVER_3 = 66;

    private static final long TOCK = 125;

    private SynchronizedSharedObjectPool<String, SharedCounter, PooledCounter> pool;


    @Before
    public void setUp() {
        this.pool = this.buildPool();
    }


    @After
    public void tearDown() {
        this.pool.dispose();
        this.pool = null;
    }


    private SynchronizedSharedObjectPool<String, SharedCounter, PooledCounter> buildPool() {
        PooledObjectFactory<String, PooledCounter> pof = new PooledCounterFactory();
        SharedObjectFactory<PooledCounter, SharedCounter> sof = SynchronizedSharedObject.factory(SharedCounter.class);
        EvictionPolicy evictionPolicy = new ImmediateEvictionPolicy();

        return new SynchronizedSharedObjectPool.Builder<String, SharedCounter, PooledCounter>()
                .setPooledObjectFactory(pof)
                .setSharedObjectFactory(sof)
                .setEvictionPolicy(evictionPolicy)
                .setIdleDisposeTimeMillis(TICK)
                .setDisposeThreads(1)
                .build();
    }


    // Get a shared object from the pool and dispose of it.
    @Test
    public void testGetAndDispose() throws Exception {
        // Get a shared object.
        SharedCounter cnt = this.pool.get("AAA");
        assertEquals(1, cnt.increment());
        assertEquals(1, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(1, this.pool.getPooledObjectsCount());

        // Dispose of the shared object.
        // Since the disposal is asynchronous, immediately afterwards the pooled object is still available.
        cnt.dispose();
        assertEquals(0, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(1, this.pool.getPooledObjectsCount());
        assertTrue(this.pool.containsPooledObject("AAA"));

        // Wait for the pooled objected to be disposed of.
        Thread.sleep(TOCK);

        // The pooled object is disposed of.
        assertEquals(0, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(0, this.pool.getPooledObjectsCount());
        assertFalse(this.pool.containsPooledObject("AAA"));
    }


    // Get a shared object with the same key multiple times, and dispose of the shared objects.
    @Test
    public void testGetSameShared() throws Exception {
        // Get a shared object.
        SharedCounter first = this.pool.get("AAA");
        assertEquals(1, first.increment());
        assertEquals(1, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(1, this.pool.getPooledObjectsCount());

        // Get a second shared object with the same key. Objects are sharing the same underlying pooled object.
        SharedCounter second = this.pool.get("AAA");
        assertEquals(2, second.increment());
        assertEquals(2, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(1, this.pool.getPooledObjectsCount());

        // Dispose of the first shared object.
        first.dispose();
        assertEquals(1, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(1, this.pool.getPooledObjectsCount());
        assertTrue(this.pool.containsPooledObject("AAA"));

        // We still keep the second shared object, so the pooled object is not disposed of even after enough time.
        Thread.sleep(TOCK);
        assertEquals(1, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(1, this.pool.getPooledObjectsCount());
        assertTrue(this.pool.containsPooledObject("AAA"));

        // Dispose of the second shared object.
        // Since the disposal is asynchronous, immediately afterwards the pooled object is still available.
        second.dispose();
        assertEquals(0, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(1, this.pool.getPooledObjectsCount());
        assertTrue(this.pool.containsPooledObject("AAA"));

        // Wait for the pooled objected to be disposed of.
        Thread.sleep(TOCK);

        // The pooled object is disposed of.
        assertEquals(0, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(0, this.pool.getPooledObjectsCount());
        assertFalse(this.pool.containsPooledObject("AAA"));
    }


    // Get a shared object when a disposal delay is running and keep the shared object. This cancel the waiting task
    // to dispose of the pooled object.
    @Test
    public void testGetAndKeepSharedWhileDisposing() throws Exception {
        // Get a shared object.
        SharedCounter first = this.pool.get("AAA");
        assertEquals(1, first.increment());
        assertEquals(1, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(1, this.pool.getPooledObjectsCount());

        // Dispose of the shared object.
        // Since the disposal is asynchronous, immediately afterwards the pooled object is still available.
        first.dispose();
        assertEquals(0, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(1, this.pool.getPooledObjectsCount());
        assertTrue(this.pool.containsPooledObject("AAA"));

        // Wait a bit...
        Thread.sleep(TICK_OVER_2);

        // Get a shared object with the same key. The pooled object was not disposed of yet, and is reused.
        SharedCounter second = this.pool.get("AAA");
        assertEquals(2, second.increment());
        assertEquals(1, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(1, this.pool.getPooledObjectsCount());
        assertTrue(this.pool.containsPooledObject("AAA"));

        // Wait to give enough time original attempt to dispose of the pooled object.
        // The attempt should have been cancelled.
        // We are still holding the second shared object, so the pooled object shall remain non-disposed.
        Thread.sleep(TOCK);
        assertEquals(1, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(1, this.pool.getPooledObjectsCount());
        assertTrue(this.pool.containsPooledObject("AAA"));

        // Dispose of the second shared object.
        // Since the disposal is asynchronous, immediately afterwards the pooled object is still available.
        second.dispose();
        assertEquals(0, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(1, this.pool.getPooledObjectsCount());
        assertTrue(this.pool.containsPooledObject("AAA"));

        // Wait for the pooled objected to be disposed of.
        Thread.sleep(TOCK);

        // The pooled object is disposed of.
        assertEquals(0, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(0, this.pool.getPooledObjectsCount());
        assertFalse(this.pool.containsPooledObject("AAA"));
    }


    // Get a shared object when a disposal delay is running and immediately dispose of the shared object.
    // The time delay to dispose of the pooled object is reset.
    @Test
    public void testGetAndDisposeSharedWhileDisposing() throws Exception {
        // Get a shared object.
        SharedCounter first = this.pool.get("AAA");
        assertEquals(1, first.increment());
        assertEquals(1, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(1, this.pool.getPooledObjectsCount());

        // Dispose of the shared object.
        // Since the disposal is asynchronous, immediately afterwards the pooled object is still available.
        first.dispose();
        assertEquals(0, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(1, this.pool.getPooledObjectsCount());
        assertTrue(this.pool.containsPooledObject("AAA"));

        // Wait a bit...
        Thread.sleep(TICK_OVER_2);

        // Get a shared object with the same key. The pooled object was not disposed of yet, and is reused.
        SharedCounter second = this.pool.get("AAA");
        assertEquals(2, second.increment());
        assertEquals(1, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(1, this.pool.getPooledObjectsCount());
        assertTrue(this.pool.containsPooledObject("AAA"));

        // Immediately dispose of the second object.
        // Since the disposal is asynchronous, immediately afterwards the pooled object is still available.
        second.dispose();
        assertEquals(0, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(1, this.pool.getPooledObjectsCount());
        assertTrue(this.pool.containsPooledObject("AAA"));

        // Wait long enough for the delay after the first dispose to expire, but not enough after the second dispose.
        // Since getting the second shared object reset the delay, the pooled object still remains available.
        Thread.sleep(TICK_2_OVER_3);
        assertEquals(0, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(1, this.pool.getPooledObjectsCount());
        assertTrue(this.pool.containsPooledObject("AAA"));

        // Wait for the pooled objected to be disposed of.
        Thread.sleep(TOCK);

        // The pooled object is disposed of.
        assertEquals(0, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(0, this.pool.getPooledObjectsCount());
        assertFalse(this.pool.containsPooledObject("AAA"));
    }
}
