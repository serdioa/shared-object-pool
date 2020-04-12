package de.serdioa.common.pool;


@FunctionalInterface
public interface PooledObjectCreator<K, P> {
    P create(K key) throws InvalidKeyException;
}
