package de.serdioa.common.pool;


public interface SharedObjectPool<K, S extends SharedObject> {
    S get(K key) throws InvalidKeyException, InitializationException;
}
