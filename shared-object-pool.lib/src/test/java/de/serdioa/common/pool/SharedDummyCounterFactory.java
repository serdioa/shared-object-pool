package de.serdioa.common.pool;


public class SharedDummyCounterFactory implements SharedObjectFactory<PooledDummyCounter, SharedDummyCounter> {
    @Override
    public SharedDummyCounter createShared(PooledDummyCounter pooledObject, Runnable disposeCallback) {
        return new SharedDummyCounter(pooledObject, disposeCallback);
    }
}
