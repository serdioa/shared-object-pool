package de.serdioa.common.pool;


/**
 * A function to create new instances of pooled objects contained by a {@link SharedObjectPool}. The function may be
 * used to compose a {@link PooledObjectFactory} from external functions by using the
 * {@link DefaultPooledObjectFactory}.
 *
 * @param <K> type of keys used to create pooled objects.
 * @param <P> type of pooled objects created by this factory.
 */
@FunctionalInterface
public interface PooledObjectCreator<K, P> {

    /**
     * Creates a new pooled object for the specified key. Many implementations of the pool execute this method in a
     * section which requires an exclusive access to the pool, so this method shall be as fast as possible, delaying any
     * time-consuming initialization to the method {@link PooledObjectFactory#initialize(Object)}.
     * <p>
     * It is highly recommended that this method just stores some internal variables on the provided pooled object,
     * postponing any actual initialization to the method {@link PooledObjectFactory#initialize(Object)}.
     *
     * @param key the key to create a pooled object for.
     * @return the created pooled object.
     *
     * @throws InvalidKeyException if the provided key is invalid, and the factory could not create a pooled object for
     * it.
     */
    P create(K key) throws InvalidKeyException;
}
