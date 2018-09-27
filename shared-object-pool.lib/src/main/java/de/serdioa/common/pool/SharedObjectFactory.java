package de.serdioa.common.pool;


public interface SharedObjectFactory<P extends PooledObject, S extends SharedObject> {
    S createShared(P pooledObject, Runnable disposeCallback);
}
