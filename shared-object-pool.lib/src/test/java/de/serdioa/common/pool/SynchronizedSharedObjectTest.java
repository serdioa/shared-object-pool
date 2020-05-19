package de.serdioa.common.pool;

import de.serdioa.common.pool.sample.PooledCounter;
import de.serdioa.common.pool.sample.SharedCounter;


/**
 * Unit tests for reflection-based {@link SynchronizedSharedObject}.
 */
public class SynchronizedSharedObjectTest extends AbstractSharedObjectTest {

    @Override
    protected SharedObjectFactory<PooledCounter, SharedCounter> sharedObjectFactory() {
        return SynchronizedSharedObject.factory(SharedCounter.class);
    }
}
