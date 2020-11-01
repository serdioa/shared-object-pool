package de.serdioa.common.pool;


/**
 * A factory for managing life cycle of shared objects. A life cycle of a shared object includes the following phases:
 * <ul>
 * <li>A factory creates a shared object from a pooled object using the method
 * {@link #createShared(java.lang.Object, java.lang.Runnable)}, the created shared object is provided to a client.
 * <li>Once the client is done using the shared object, the client {@link SharedObject#dispose() disposes of it}.
 * </ul>
 *
 * @param <P> the type of the pooled object.
 * @param <S> the type of the shared object.
 */
public interface SharedObjectFactory<P, S extends SharedObject> {
    /**
     * Creates a shared object from the provided pooled object. When client disposes of the created shared object,
     * the shared object shall invoke the provided {@code disposeCallback}, informing the pool that the shared object
     * is not used anymore.
     *
     * @param pooledObject the pooled object for which a shared object to be created.
     * @param disposeCallback the callback to be called when the shared object is disposed of.
     *
     * @return a new shared object.
     */
    S createShared(P pooledObject, Runnable disposeCallback);
}
