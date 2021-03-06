package de.serdioa.common.pool;

import de.serdioa.common.pool.sample.PooledCounter;
import de.serdioa.common.pool.sample.SharedCounter;


/**
 * Unit tests for reflection-based {@link LockingSharedObject}.
 */
public class LockingSharedObjectTest extends AbstractSharedObjectTest {

    @Override
    protected SharedObjectFactory<PooledCounter, SharedCounter> sharedObjectFactory() {
        return LockingSharedObject.factory(SharedCounter.class);
    }
}
