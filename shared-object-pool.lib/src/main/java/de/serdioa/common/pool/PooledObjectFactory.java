package de.serdioa.common.pool;


/**
 * A factory for managing lifecycle of a pooled object. A shared object pool uses a factory to create new pooled
 * objects on demand, and to dispose of pooled objects when they are not required anymore.
 *
 * @param <K> the type of keys used to create pooled objects.
 * @param <P> the type of pooled objects created by this factory.
 */
public interface PooledObjectFactory<K, P> {
    /**
     * Creates a new pooled object for the specified key. Many implementations of the pool execute this method in
     * a section which requires an exclusive access to the pool, so this method shall be as fast as possible, delaying
     * any time-consuming initialization to the method {@link #initialize(Object)}.
     * <p>
     * It is highly recommended that this method just stores some internal variables on the provided pooled object,
     * postponing any actual initialization to the method {@link #initialize(Object)}.
     *
     * @param key the key to create a pooled object for.
     * @return the created pooled object.
     *
     * @throws InvalidKeyException if the provided key is invalid, and the factory could not create a pooled object
     * for it.
     */
    P create(K key) throws InvalidKeyException;


    /**
     * Initializes the provided pooled object created by this factory. This method is guaranteed to be called before
     * the pooled object is used, and it is guaranteed to be called at most once.
     * <p>
     * This method could execute a time-consuming initialization, such as loading data from external data sources.
     * Still, if loading external data takes especially long, it makes more sense to add to the object an externally
     * visible state, initially marking the object as "stale", loading external data in a background thread, and marking
     * the object as "active" once all required data is loaded.
     *
     * @param pooledObject the new non-initialized pooled object created by this factory.
     */
    void initialize(P pooledObject);


    /**
     * Dispose of the provided pooled object created by this factory. This method is guaranteed to be invoked at most
     * once, and only on pooled objects which were initialized before.
     * <p>
     * This method is expected to remove any listeners the object has registered, to disconnect from any external data
     * sources and to dispose of any resources used by the pooled object.
     *
     * @param pooledObject the pooled object to dispose of.
     */
    void dispose(P pooledObject);
}
