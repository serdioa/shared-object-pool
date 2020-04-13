package de.serdioa.common.pool;

import de.serdioa.common.pool.sample.PooledCounter;


/**
 * Unit tests for reflection-based {@link SynchronizedSharedObject}.
 */
public class SynchronizedSharedObjectTest extends AbstractSharedObjectTest {

    @Override
    protected SharedObjectFactory<PooledCounter, ? extends SharedCounter> sharedObjectFactory() {
        return SynchronizedSharedObject.factory(SharedCounter.class);
    }
}
