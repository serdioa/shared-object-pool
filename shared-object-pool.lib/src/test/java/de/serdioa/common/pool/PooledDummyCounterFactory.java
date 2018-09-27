package de.serdioa.common.pool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PooledDummyCounterFactory implements PooledObjectFactory<String, PooledDummyCounter> {
    private static final Logger logger = LoggerFactory.getLogger(PooledDummyCounterFactory.class);

    @Override
    public PooledDummyCounter create(String key) throws InvalidKeyException {
        logger.trace("PooledDummyCounterFactory creating PooledDummyCounter[{}]", key);
        return new PooledDummyCounter(key);
    }
}
