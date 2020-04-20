package de.serdioa.common.pool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import de.serdioa.common.pool.sample.PooledCounter;
import de.serdioa.common.pool.sample.SharedCounter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * Base class for unit tests for various implementations of an {@link AbstractSharedObjectPool}.
 */
public abstract class AbstractSharedObjectPoolTest {

    protected AbstractSharedObjectPool<String, SharedCounter, PooledCounter> pool;


    protected abstract AbstractSharedObjectPool<String, SharedCounter, PooledCounter> buildPool();


    @Before
    public void setUp() {
        this.pool = buildPool();
    }


    @After
    public void tearDown() {
        if (this.pool != null) {
            this.pool.dispose();
            this.pool = null;
        }
    }


    @Test
    public void testGetSingle() {
        // Get a shared object and use it.
        SharedCounter shared = this.pool.get("AAA");
        assertNotNull(shared);
        assertEquals(1, shared.increment());

        // Dispose of the instance.
        shared.dispose();
    }


    @Test
    public void testGetMultipleSameKey() {
        // Get several instance of shared object with the same key.
        SharedCounter firstShared = this.pool.get("AAA");
        assertNotNull(firstShared);
        assertEquals(1, firstShared.increment());

        // Get another instance with the same key without disposing the first instance.
        // The object shall be shared, that is the counter shall be the same as the first one.
        SharedCounter secondShared = this.pool.get("AAA");
        assertNotNull(secondShared);
        assertEquals(2, secondShared.increment());

        // Dispose of both instances.
        firstShared.dispose();
        secondShared.dispose();
    }


    @Test
    public void testGetMultipleDifferentKeys() {
        // Get several shared objects with different keys.
        SharedCounter firstShared = this.pool.get("AAA");
        assertNotNull(firstShared);
        assertEquals(1, firstShared.increment());

        // Get a shared object with a different key. The counter shall be independent of the first one.
        SharedCounter secondShared = this.pool.get("BBB");
        assertNotNull(secondShared);
        assertEquals(1, secondShared.increment());

        // Dispose of both instances.
        firstShared.dispose();
        secondShared.dispose();
    }
}