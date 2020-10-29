package de.serdioa.common.pool;

import static org.junit.Assert.assertEquals;

import de.serdioa.common.pool.sample.PooledCounter;
import de.serdioa.common.pool.sample.PooledCounterFactory;
import de.serdioa.common.pool.sample.SharedCounter;
import org.junit.Test;


public class LockingSharedObjectPoolTest extends AbstractSharedObjectPoolTest {

    @Override
    protected LockingSharedObjectPool<String, SharedCounter, PooledCounter> buildPool() {
        PooledObjectFactory<String, PooledCounter> pof = new PooledCounterFactory();
        SharedObjectFactory<PooledCounter, SharedCounter> sof = LockingSharedObject.factory(SharedCounter.class);

        return new LockingSharedObjectPool.Builder<String, SharedCounter, PooledCounter>()
                .setPooledObjectFactory(pof)
                .setSharedObjectFactory(sof)
                .build();
    }


    @Test
    public void testDisposeEmptyPool() {
        // Get several shared objects with different keys.
        SharedCounter firstShared = this.pool.get("AAA");
        SharedCounter secondShared = this.pool.get("BBB");

        // The pool contains some pooled objects now.
        assertEquals(2, this.pool.getPooledObjectsCount());

        // Dispose of the shared objects.
        firstShared.dispose();
        secondShared.dispose();

        // The pool does not contain any pooled objects now.
        assertEquals(0, this.pool.getPooledObjectsCount());

        // Dispose of the pool.
        this.pool.dispose();

        // The disposed pool does not contain any pooled objects.
        assertEquals(0, this.pool.getPooledObjectsCount());
    }


    @Test
    public void testDisposePoolWithObjects() {
        // Get several shared objects with different keys.
        SharedCounter firstShared = this.pool.get("AAA");
        SharedCounter secondShared = this.pool.get("BBB");

        // The pool contains some pooled objects now.
        assertEquals(2, this.pool.getPooledObjectsCount());

        // Dispose of the pool.
        this.pool.dispose();

        // The disposed pool does not contain any pooled objects, that is pooled objects were disposed of.
        assertEquals(0, this.pool.getPooledObjectsCount());
    }


    @Test(expected = IllegalStateException.class)
    public void testGetFromDisposedPool() {
        this.pool.dispose();

        // An attempt to get an object from a disposed pool shall throw an IllegalStateException.
        this.pool.get("AAA");
    }
}
