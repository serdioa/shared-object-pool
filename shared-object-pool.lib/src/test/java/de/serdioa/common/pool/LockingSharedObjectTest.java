package de.serdioa.common.pool;

import de.serdioa.common.pool.sample.PooledCounter;


/**
 * Unit tests for reflection-based {@link LockingSharedObject}.
 */
public class LockingSharedObjectTest extends AbstractSharedObjectTest {

    @Override
    protected SharedObjectFactory<PooledCounter, ? extends SharedCounter> sharedObjectFactory() {
        return LockingSharedObject.factory(SharedCounter.class);
    }
}
