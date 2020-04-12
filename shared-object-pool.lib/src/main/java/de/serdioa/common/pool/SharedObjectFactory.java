package de.serdioa.common.pool;


public interface SharedObjectFactory<P, S extends SharedObject> {
    S createShared(P pooledObject, Runnable disposeCallback);
}
