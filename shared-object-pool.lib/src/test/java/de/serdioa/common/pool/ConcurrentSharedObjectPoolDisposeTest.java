package de.serdioa.common.pool;

import static org.junit.Assert.assertEquals;

import java.lang.ref.WeakReference;

import de.serdioa.common.pool.sample.PooledCounter;
import de.serdioa.common.pool.sample.PooledCounterFactory;
import de.serdioa.common.pool.sample.SharedCounter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class ConcurrentSharedObjectPoolDisposeTest {

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
                .build();
    }


    private void forceGc() {
        Object obj = new Object();
        WeakReference<Object> ref = new WeakReference<>(obj);
        obj = null;

        while (ref.get() != null) {
            System.gc();
        }
    }


    // Get a shared object from the pool and dispose of it.
    // Expect the pool to not contain any pooled objects afterwards.
    @Test
    public void testGetAndDispose() {
        SharedCounter cnt = this.pool.get("AAA");
        assertEquals(1, cnt.increment());
        assertEquals(1, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(1, this.pool.getPooledObjectsCount());

        cnt.dispose();
        assertEquals(0, this.pool.getPooledObjectsCount());
    }


    // Get a shared object from the pool, but "forget" to dispose of it.
    // Give GC a chance to dispose of the shared object via the phantom reference.
    @Test
    public void testGetAndGcDispose() throws InterruptedException {
        SharedCounter cnt = this.pool.get("AAA");
        assertEquals(1, cnt.increment());
        assertEquals(1, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(1, this.pool.getPooledObjectsCount());

        // We "forget" to dispose of the shared object, and let the GC to do it.
        // Give JVM some time to dispose of the object via the phantom reference.
        cnt = null;
        forceGc();
        Thread.sleep(500);

        assertEquals(0, this.pool.getPooledObjectsCount());
    }


    // Get a shared object with the same key multiple times, and dispose of the shared objects.
    @Test
    public void testGetSameShared() {
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

        // Dispose of the second shared object.
        second.dispose();
        assertEquals(0, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(0, this.pool.getPooledObjectsCount());
    }


    // Get a shared object with the same key multiple times, but "forget" to dispose of it.
    // Give GC a chance to dispose of the shared object via the phantom reference.
    @Test
    public void testGetSameSharedGcDispose() throws InterruptedException {
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

        // We "forget" to dispose of the first shared object, and let the GC to do it.
        // Give JVM some time to dispose of the object via the phantom reference.
        first = null;
        forceGc();
        Thread.sleep(500);

        assertEquals(1, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(1, this.pool.getPooledObjectsCount());

        // We "forget" to dispose of the second shared object as well, and let the GC to do it.
        // Give JVM some time to dispose of the object via the phantom reference.
        second = null;
        forceGc();
        Thread.sleep(500);

        assertEquals(0, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(0, this.pool.getPooledObjectsCount());
    }

    // Get shared objects with different keys, and dispose of the shared objects.
    @Test
    public void testGetDifferentShared() {
        // Get a shared object.
        SharedCounter first = this.pool.get("AAA");
        assertEquals(1, first.increment());
        assertEquals(1, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(0, this.pool.getSharedObjectsCount("BBB"));
        assertEquals(1, this.pool.getPooledObjectsCount());

        // Get a shared object with a different key.
        SharedCounter second = this.pool.get("BBB");
        assertEquals(1, second.increment());
        assertEquals(1, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(1, this.pool.getSharedObjectsCount("BBB"));
        assertEquals(2, this.pool.getPooledObjectsCount());

        // Dispose of the first shared object.
        first.dispose();
        assertEquals(0, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(1, this.pool.getSharedObjectsCount("BBB"));
        assertEquals(1, this.pool.getPooledObjectsCount());

        // Dispose of the second shared object.
        second.dispose();
        assertEquals(0, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(0, this.pool.getSharedObjectsCount("BBB"));
        assertEquals(0, this.pool.getPooledObjectsCount());
    }


    // Get shared objects with different keys, but "forget" to dispose of them.
    // Give GC a chance to dispose of the shared object via the phantom reference.
    @Test
    public void testGetDifferentSharedGcDispose() throws InterruptedException {
        // Get a shared object.
        SharedCounter first = this.pool.get("AAA");
        assertEquals(1, first.increment());
        assertEquals(1, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(0, this.pool.getSharedObjectsCount("BBB"));
        assertEquals(1, this.pool.getPooledObjectsCount());

        // Get a shared object with a different key.
        SharedCounter second = this.pool.get("BBB");
        assertEquals(1, second.increment());
        assertEquals(1, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(1, this.pool.getSharedObjectsCount("BBB"));
        assertEquals(2, this.pool.getPooledObjectsCount());

        // We "forget" to dispose of the first shared object, and let the GC to do it.
        // Give JVM some time to dispose of the object via the phantom reference.
        first = null;
        forceGc();
        Thread.sleep(500);

        assertEquals(0, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(1, this.pool.getSharedObjectsCount("BBB"));
        assertEquals(1, this.pool.getPooledObjectsCount());

        // We "forget" to dispose of the second shared object as well, and let the GC to do it.
        // Give JVM some time to dispose of the object via the phantom reference.
        second = null;
        forceGc();
        Thread.sleep(500);

        assertEquals(0, this.pool.getSharedObjectsCount("AAA"));
        assertEquals(0, this.pool.getSharedObjectsCount("BBB"));
        assertEquals(0, this.pool.getPooledObjectsCount());
    }
}
