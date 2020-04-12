package de.serdioa.common.pool;


public abstract class AbstractSharedObjectPool<K, S extends SharedObject, P> implements SharedObjectPool<K, S> {

    // Factory for creating new pooled objects.
    private PooledObjectFactory<K, P> pooledObjectFactory;

    // Factory for creating shared objects from pooled objects.
    private SharedObjectFactory<P, S> sharedObjectFactory;


    public void setPooledObjectFactory(PooledObjectFactory<K, P> pooledObjectFactory) {
        this.pooledObjectFactory = pooledObjectFactory;
    }


    public void setSharedObjectFactory(SharedObjectFactory<P, S> sharedObjectFactory) {
        this.sharedObjectFactory = sharedObjectFactory;
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
