package de.serdioa.common.pool;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;


public abstract class AbstractSharedObjectPool<K, S extends SharedObject, P> implements SharedObjectPool<K, S> {

    // A static counter used to create unique names for object pools.
    private static final AtomicInteger NAME_COUNTER = new AtomicInteger();

    // Name of this object pool used for logging and naming threads.
    protected final String name;

    // Factory for creating new pooled objects.
    private final PooledObjectFactory<K, P> pooledObjectFactory;

    // Factory for creating shared objects from pooled objects.
    private final SharedObjectFactory<P, S> sharedObjectFactory;


    protected AbstractSharedObjectPool(final String name,
            PooledObjectFactory<K, P> pooledObjectFactory,
            SharedObjectFactory<P, S> sharedObjectFactory) {

        this.name = (name != null ? null : this.getClass().getSimpleName() + '-' + NAME_COUNTER.getAndIncrement());
        this.pooledObjectFactory = Objects.requireNonNull(pooledObjectFactory);
        this.sharedObjectFactory = Objects.requireNonNull(sharedObjectFactory);
    }


    public String getName() {
        return this.name;
    }


    public void dispose() {
        // May be implemented in a derived class.
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
