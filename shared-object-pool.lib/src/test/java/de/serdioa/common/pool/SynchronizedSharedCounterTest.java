package de.serdioa.common.pool;

import de.serdioa.common.pool.sample.PooledCounter;
import de.serdioa.common.pool.sample.SharedCounter;
import de.serdioa.common.pool.sample.SynchronizedSharedCounter;


/**
 * Unit tests for an manually written {@link SynchronizedSharedCounter}.
 */
public class SynchronizedSharedCounterTest extends AbstractSharedObjectTest {

    @Override
    protected SharedObjectFactory<PooledCounter, SharedCounter> sharedObjectFactory() {
        return SynchronizedSharedCounter.factory();
    }
}
