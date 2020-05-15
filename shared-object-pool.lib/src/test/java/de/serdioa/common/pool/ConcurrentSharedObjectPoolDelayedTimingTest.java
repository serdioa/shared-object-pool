package de.serdioa.common.pool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.ref.WeakReference;

import de.serdioa.common.pool.sample.PooledCounter;
import de.serdioa.common.pool.sample.PooledCounterFactory;
import de.serdioa.common.pool.sample.SharedCounter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class ConcurrentSharedObjectPoolDelayedTimingTest {

    private static final long TICK = 100;
    private static final long TICK_OVER_2 = 50;
    private static final long TICK_OVER_3 = 33;
    private static final long TICK_2_OVER_3 = 66;

    private static final long TOCK = 125;

    private ConcurrentSharedObjectPool<String, SharedCounter, PooledCounter> pool;


    @Before
    public void setUp() {
        this.pool = this.buildPool();
    }


    @After
    public void tearDown() {
        this.pool.dispose();
        this.pool = null;
    }


    private ConcurrentSharedObjectPool<String, SharedCounter, PooledCounter> buildPool() {
        PooledObjectFactory<String, PooledCounter> pof = new PooledCounterFactory();
        SharedObjectFactory<PooledCounter, SharedCounter> sof = SynchronizedSharedObject.factory(SharedCounter.class);

        return new ConcurrentSharedObjectPool.Builder<String, SharedCounter, PooledCounter>()
                .setPooledObjectFactory(pof)
                .setSharedObjectFactory(sof)
                .setStackTraceProvider(new NoOpStackTraceProvider())
                .setIdleDisposeTimeMillis(TICK)
                .setDisposeThreads(1)
                .build();
    }


    private static void forceGc() {
        Object obj = new Object();
        WeakReference<Object> ref = new WeakReference<>(obj);
        obj = null;

        while (ref.get() != null) {
            System.gc();
        }
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


    // Get a shared object "forget" to dispose of it.
    // Give GC a chance to dispose of the shared object via the phantom reference.
    @Test
    public void testGetAndGcDispose() throws InterruptedException {
        SharedCounter cnt = this.pool.get("AAA");
        assertEquals(1, cnt.increment());
        assertEquals(1, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(1, this.pool.getPooledObjectsCount());

        // We "forget" to dispose of the shared object, and let the GC to do it.
        // Give JVM some time to GC the object, and to the reaper to note via the phantom reference that the shared
        // object has been GC'ed without being properly disposed of.
        cnt = null;
        forceGc();
        Thread.sleep(TICK_OVER_2);

        // Since the disposal is asynchronous, immediately afterwards the pooled object is still available.
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


    // Get a shared object and immediately dispose of it. Before the waiting time expires and the pooled object
    // is disposed of, force GC.
    @Test
    public void testGetDisposeAndGcDispose() throws InterruptedException {
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

        // Wait a bit, but less than the wait time for the disposing of the pooled object.
        Thread.sleep(TICK_OVER_3);

        // Null the reference on the shared object and force GC.
        // Give JVM some time to GC the object. We do not expect the reaper thread to "notice" it, because we have
        // already disposed of the shared object properly, and tracking via the phantom reference should have been
        // deactivated.
        cnt = null;
        forceGc();
        Thread.sleep(TICK_OVER_3);

        // Since the disposal is asynchronous, the pooled object is still available.
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
