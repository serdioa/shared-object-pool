package de.serdioa.common.pool;


/**
 * A pool of objects available by key, where each object in a pool may be simultaneously used by multiple clients. A
 * client calls the method {@link #get(Object)} to obtain a shared object from the pool. The pool returns a wrapper
 * backed by a cached implementation, creating and caching a new backing implementation, if none exist yet. When a
 * client is done using the shared object, it invokes on it the method {@link SharedObject#dispose()}, informing the
 * pool that the shared object is not required anymore. The pool may track usage of shared objects, removing the backing
 * implementation from the cache once no client is using it.
 *
 * @param <K> the type of keys used to access shared objects.
 * @param <S> the type of the shared object provided by this pool.
 */
public interface SharedObjectPool<K, S extends SharedObject> {

    /**
     * Returns a shared object for the specified key.
     *
     * @param key the key to return a shared object for.
     * @return the shared object for the specified key.
     *
     * @throws InvalidKeyException if the provided key is invalid, and the pool could not provide a shared object for
     * it.
     * @throws InitializationException if the backing implementation object could not be initialized.
     */
    S get(K key) throws InvalidKeyException, InitializationException;
}
