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

    S createShared(P pooledObject, Runnable disposeCallback);
}
