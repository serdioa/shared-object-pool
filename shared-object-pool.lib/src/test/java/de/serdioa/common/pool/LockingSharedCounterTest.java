package de.serdioa.common.pool;

import de.serdioa.common.pool.sample.LockingSharedCounter;
import de.serdioa.common.pool.sample.PooledCounter;
import de.serdioa.common.pool.sample.SharedCounter;


/**
 * Unit tests for an manually written {@link LockingSharedCounter}.
 */
public class LockingSharedCounterTest extends AbstractSharedObjectTest {

    @Override
    protected SharedObjectFactory<PooledCounter, SharedCounter> sharedObjectFactory() {
        return LockingSharedCounter.factory();
    }
}
