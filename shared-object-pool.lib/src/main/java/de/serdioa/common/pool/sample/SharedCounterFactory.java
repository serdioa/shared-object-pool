package de.serdioa.common.pool.sample;

import de.serdioa.common.pool.SharedObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SharedCounterFactory implements SharedObjectFactory<PooledCounter, SharedCounter> {
    private static final Logger logger = LoggerFactory.getLogger(PooledCounterFactory.class);

    @Override
    public SharedCounter createShared(PooledCounter pooledObject, Runnable disposeCallback) {
        logger.trace("SharedCounterFactory creating SharedCounter[{}]", pooledObject.getKey());
        return new SharedCounter(pooledObject, disposeCallback);
    }
}
