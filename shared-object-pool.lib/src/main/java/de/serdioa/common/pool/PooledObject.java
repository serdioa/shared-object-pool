package de.serdioa.common.pool;


public interface PooledObject {
    void init() throws InitializationException;


    void dispose();
}
