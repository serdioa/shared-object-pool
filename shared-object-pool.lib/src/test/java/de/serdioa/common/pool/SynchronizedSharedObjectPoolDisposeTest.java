package de.serdioa.common.pool;

import static org.junit.Assert.assertEquals;

import de.serdioa.common.pool.sample.PooledCounter;
import de.serdioa.common.pool.sample.PooledCounterFactory;
import de.serdioa.common.pool.sample.SharedCounter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class SynchronizedSharedObjectPoolDisposeTest {

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

        return new SynchronizedSharedObjectPool.Builder<String, SharedCounter, PooledCounter>()
                .setPooledObjectFactory(pof)
                .setSharedObjectFactory(sof)
                .build();
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
}
