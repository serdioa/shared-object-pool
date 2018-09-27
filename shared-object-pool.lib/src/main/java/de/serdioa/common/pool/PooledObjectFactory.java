package de.serdioa.common.pool;


public interface PooledObjectFactory<K, P extends PooledObject> {
    P create(K key) throws InvalidKeyException;
}
