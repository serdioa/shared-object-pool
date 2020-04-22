package de.serdioa.common.pool;

import de.serdioa.common.pool.sample.PooledCounter;
import de.serdioa.common.pool.sample.PooledCounterFactory;
import de.serdioa.common.pool.sample.SharedCounter;


public class ConcurrentSharedObjectPoolTest extends AbstractSharedObjectPoolTest {

    @Override
    protected ConcurrentSharedObjectPool<String, SharedCounter, PooledCounter> buildPool() {
        PooledObjectFactory<String, PooledCounter> pof = new PooledCounterFactory();
        SharedObjectFactory<PooledCounter, SharedCounter> sof = LockingSharedObject.factory(SharedCounter.class);
        EvictionPolicy evictionPolicy = new ImmediateEvictionPolicy();

        return new ConcurrentSharedObjectPool.Builder<String, SharedCounter, PooledCounter>()
                .setPooledObjectFactory(pof)
                .setSharedObjectFactory(sof)
                .setEvictionPolicy(evictionPolicy)
                .build();
    }
}
