package de.serdioa.common.pool;

import de.serdioa.common.pool.sample.PooledCounter;
import de.serdioa.common.pool.sample.PooledCounterFactory;
import de.serdioa.common.pool.sample.SharedCounter;


public class SynchronizedSharedObjectPoolTest extends AbstractSharedObjectPoolTest {

    @Override
    protected SynchronizedSharedObjectPool<String, SharedCounter, PooledCounter> buildPool() {
        PooledObjectFactory<String, PooledCounter> pof = new PooledCounterFactory();
        SharedObjectFactory<PooledCounter, SharedCounter> sof = SynchronizedSharedObject.factory(SharedCounter.class);
        EvictionPolicy evictionPolicy = new ImmediateEvictionPolicy();

        return new SynchronizedSharedObjectPool.Builder<String, SharedCounter, PooledCounter>()
                .setPooledObjectFactory(pof)
                .setSharedObjectFactory(sof)
                .setEvictionPolicy(evictionPolicy)
                .build();
    }
}
