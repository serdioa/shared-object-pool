package de.serdioa.common.pool.sample;

import de.serdioa.common.pool.InvalidKeyException;
import de.serdioa.common.pool.PooledObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PooledCounterFactory implements PooledObjectFactory<String, PooledCounter> {

    private static final Logger logger = LoggerFactory.getLogger(PooledCounterFactory.class);


    @Override
    public PooledCounter create(String key) throws InvalidKeyException {
        logger.trace("PooledCounterFactory creating PooledCounter[{}]", key);
        return new PooledCounter(key);
    }


    @Override
    public void initialize(PooledCounter pooledObject) {
        pooledObject.init();
    }


    @Override
    public void dispose(PooledCounter pooledObject) {
        pooledObject.dispose();
    }
}
