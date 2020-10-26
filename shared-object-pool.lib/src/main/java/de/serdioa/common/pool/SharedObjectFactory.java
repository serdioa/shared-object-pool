package de.serdioa.common.pool;


/**
 * A factory for managing life cycle of shared objects. A life cycle of a shared object includes the following phases:
 * <ul>
 * <li>A factory creates a shared object from a pooled object using the method
 * {@link #createShared(java.lang.Object, java.lang.Runnable)}, the created shared object is provided to a client.
 * <li>Once the client is done using the shared object, the client {@link SharedObject#dispose() disposes of it}.
 * <li>Alternatively, if the whole shared object pool is disposed of, the pool disposes of the shared object using the
 * method {@link #disposeByPool(SharedObject)}.
 * </ul>
 * Using different methods for a client-driven disposal vs. pool-driven disposal allows to produce more precise
 * exceptions when a shared object is used after being disposed of.
 *
 * @param <P> the type of the pooled object.
 * @param <S> the type of the shared object.
 */
public interface SharedObjectFactory<P, S extends SharedObject> {

    S createShared(P pooledObject, Runnable disposeCallback);


    default void disposeByPool(S sharedObject) {
        sharedObject.dispose();
    }
}
