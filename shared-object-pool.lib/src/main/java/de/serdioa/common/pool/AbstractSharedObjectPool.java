package de.serdioa.common.pool;

import java.util.Objects;


public abstract class AbstractSharedObjectPool<K, S extends SharedObject, P> implements SharedObjectPool<K, S> {

    // Factory for creating new pooled objects.
    private final PooledObjectFactory<K, P> pooledObjectFactory;

    // Factory for creating shared objects from pooled objects.
    private final SharedObjectFactory<P, S> sharedObjectFactory;

    // The policy for evicting non-used pooled objects.
    protected final EvictionPolicy evictionPolicy;


    protected AbstractSharedObjectPool(PooledObjectFactory<K, P> pooledObjectFactory,
            SharedObjectFactory<P, S> sharedObjectFactory,
            EvictionPolicy evictionPolicy) {
        this.pooledObjectFactory = Objects.requireNonNull(pooledObjectFactory);
        this.sharedObjectFactory = Objects.requireNonNull(sharedObjectFactory);
        this.evictionPolicy = Objects.requireNonNull(evictionPolicy);
    }


    public void dispose() {
        this.evictionPolicy.dispose();
    }


    protected P createPooledObject(K key) throws InvalidKeyException {
        return this.pooledObjectFactory.create(key);
    }


    protected void initializePooledObject(P pooledObject) {
        this.pooledObjectFactory.initialize(pooledObject);
    }


    protected void disposePooledObject(P pooledObject) {
        this.pooledObjectFactory.dispose(pooledObject);
    }


    protected S createSharedObject(P pooledObject, Runnable disposeCallback) {
        return this.sharedObjectFactory.createShared(pooledObject, disposeCallback);
    }
}
