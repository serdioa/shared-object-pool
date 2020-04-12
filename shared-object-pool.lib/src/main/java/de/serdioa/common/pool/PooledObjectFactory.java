package de.serdioa.common.pool;


public interface PooledObjectFactory<K, P> {
    P create(K key) throws InvalidKeyException;


    void initialize(P pooledObject);


    void dispose(P pooledObject);
}
