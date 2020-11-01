package de.serdioa.common.pool;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * Wraps a {@link PooledObjectFactory} to enforce a proper lifecycle on pooled objects.
 *
 * @param <K> type of keys used to create pooled objects.
 * @param <P> type of pooled objects created by this factory.
 */
public class WrappedPooledObjectFactory<K, P> implements PooledObjectFactory<K, P> {

    // The pooled object factory wrapped by this factory.
    private final PooledObjectFactory<K, P> factory;

    // The type of the pooled object.
    private final Class<P> pooledObjectType;


    public WrappedPooledObjectFactory(PooledObjectFactory<K, P> factory, Class<P> pooledObjectType) {
        this.factory = Objects.requireNonNull(factory);
        this.pooledObjectType = Objects.requireNonNull(pooledObjectType);
    }


    @Override
    public P create(K key) throws InvalidKeyException {
        P adaptee = this.factory.create(key);

        WrappedPooledObject<K, P> invocationHandler = new WrappedPooledObject<>(key, adaptee);

        @SuppressWarnings("unchecked")
        P wrappedPooledObject = (P) Proxy.newProxyInstance(this.pooledObjectType.getClassLoader(),
                new Class<?>[]{this.pooledObjectType}, invocationHandler);

        return wrappedPooledObject;
    }


    @Override
    public void initialize(P pooledObject) {
        WrappedPooledObject<K, P> invocationHandler = getInvocationHandler(pooledObject);
        invocationHandler.initialize(this.factory);
    }


    @Override
    public void dispose(P pooledObject) {
        WrappedPooledObject<K, P> invocationHandler = getInvocationHandler(pooledObject);
        invocationHandler.dispose(this.factory);
    }


    private WrappedPooledObject<K, P> getInvocationHandler(P pooledObject) {
        if (pooledObject == null) {
            throw new IllegalArgumentException("pooledObject is null");
        }

        // We are able to properly handle only pooled objects created by this factory (wrappers), but a user may
        // provide another implementation. Check if the provided object is a pooled object wrapper returned by this
        // factory.
        InvocationHandler invocationHandler;
        try {
            invocationHandler = Proxy.getInvocationHandler(pooledObject);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Can not get invocation handler from provided object of "
                    + pooledObject.getClass(), ex);
        }

        if (invocationHandler instanceof WrappedPooledObject) {
            @SuppressWarnings("unchecked")
            WrappedPooledObject<K, P> wrapper = (WrappedPooledObject<K, P>) invocationHandler;
            return wrapper;
        } else {
            throw new IllegalArgumentException("Unexpected class of the invocation handler: "
                    + invocationHandler.getClass());
        }
    }


    private static class WrappedPooledObject<K, P> implements InvocationHandler {

        private final K key;

        // The pooled object wrapped by this object.
        // Set to null when this pooled object is disposed of.
        // @GuardedBy(this.lock)
        private P adaptee;

        // Was this pooled object already initialized?
        // @GuardedBy(this.lock)
        private boolean initialized;

        private ReadWriteLock lock = new ReentrantReadWriteLock();


        public WrappedPooledObject(K key, P adaptee) {
            this.key = Objects.requireNonNull(key);

            Lock exclusiveLock = this.lock.writeLock();
            exclusiveLock.lock();
            try {
                this.adaptee = Objects.requireNonNull(adaptee);
            } finally {
                exclusiveLock.unlock();
            }
        }


        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Lock sharedLock = this.lock.readLock();
            sharedLock.lock();
            try {
                if (!this.initialized) {
                    throw new IllegalStateException("Pooled object [" + this.key + "] has not been initialized yet");
                }
                if (this.adaptee == null) {
                    throw new IllegalStateException("Pooled object [" + this.key + "] already has been disposed of");
                }

                return method.invoke(this.adaptee, args);
            } finally {
                sharedLock.unlock();
            }
        }


        public void initialize(PooledObjectFactory<K, P> adapteeFactory) {
            Lock exclusiveLock = this.lock.writeLock();
            exclusiveLock.lock();
            try {
                if (this.adaptee == null) {
                    throw new IllegalStateException("Pooled object [" + this.key + "] already has been disposed of");
                }
                if (this.initialized) {
                    throw new IllegalStateException("Pooled object [" + this.key + "] aöready has not been initialized");
                }

                adapteeFactory.initialize(this.adaptee);
                this.initialized = true;
            } finally {
                exclusiveLock.unlock();
            }
        }


        public void dispose(PooledObjectFactory<K, P> adapteeFactory) {
            Lock exclusiveLock = this.lock.writeLock();
            exclusiveLock.lock();
            try {
                if (!this.initialized) {
                    throw new IllegalStateException("Pooled object [" + this.key + "] has not been initialized yet");
                }
                if (this.adaptee == null) {
                    throw new IllegalStateException("Pooled object [" + this.key + "] already has been disposed of");
                }

                adapteeFactory.dispose(this.adaptee);
                this.adaptee = null;
            } finally {
                exclusiveLock.unlock();
            }
        }
    }
}
