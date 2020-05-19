package de.serdioa.common.pool;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Generic reflection-based implementation of {@link SharedObject} using locks.
 */
public class LockingSharedObject implements InvocationHandler {

    private static final Logger logger = LoggerFactory.getLogger(LockingSharedObject.class);

    private static final String DISPOSE_METHOD_NAME = "dispose";
    private static final String IS_DISPOSED_METHOD_NAME = "isDisposed";

    private final Object pooledObject;
    private final Runnable disposeCallback;

    // We do not really require the shared object (a proxy based on this invocation handler), but we keep a reference
    // on it to prevent it from being garbage collected before the dispose callback is executed.
    //
    // If the shared object pool tracks how shared objects are disposed, and has a protection against forgetting
    // to properly dispose of a shared object by tracking when a shared object is GC'ed, it causes a false positive
    // when a shared object is GC'ed before this invocation handler finished executing the dispose callback. To prevent
    // a false positive, we keep a reference on the shared object until this invocation handler is disposed of.
    //
    // In addition, sharedObject == null is used as an indicator that this invocation handler has been disposed of.
    // We may have used an additional boolean variables, but there is no advantage in doing so if we have to keep
    // a reference on the shared object anyway.
    //
    // @GuardedBy(lock)
    private Object sharedObject;

    // If this shared object has been disposed of, was it disposed of by the shared objects pool?
    // @GuardedBy(lock)
    private boolean disposedByPool;

    // Synchronization lock for the lifecycle and accessing the pooled object.
    private final ReadWriteLock lock = new ReentrantReadWriteLock();


    public LockingSharedObject(Object pooledObject, Runnable disposeCallback) {
        this.pooledObject = Objects.requireNonNull(pooledObject);
        this.disposeCallback = Objects.requireNonNull(disposeCallback);
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Special methods.
        if (args == null || args.length == 0) {
            String methodName = method.getName();
            if (DISPOSE_METHOD_NAME.equals(methodName)) {
                this.dispose();
                return null;
            } else if (IS_DISPOSED_METHOD_NAME.equals(methodName)) {
                return this.isDisposed();
            }
        }

        // All other methods are forwarded to the pooled object.
        return invokePooled(method, args);
    }


    private Object invokePooled(Method method, Object[] args) throws Throwable {
        // Using readLock (shared lock): many threads may call methods of the pooled object simultaneously.
        // Of course, the pooled object may implement some synchronization itself, but for the purposes of this
        // shared object exclusion is not required.

        Lock readLock = this.lock.readLock();
        readLock.lock();
        try {
            if (this.sharedObject == null) {
                if (this.disposedByPool) {
                    throw new IllegalStateException("Method called on dynamic shared object disposed by the pool: " + method);
                } else {
                    throw new IllegalStateException("Method called on disposed dynamic shared object: " + method);
                }
            }

            return method.invoke(this.pooledObject, args);
        } finally {
            readLock.unlock();
        }
    }


    private void dispose() {
        // Dispose of this shared object.
        // Using writeLock (exclusive lock) to prevent parallel lifecycle events and/or usage of this shared object.
        Lock writeLock = this.lock.writeLock();
        writeLock.lock();
        try {
            if (this.sharedObject == null) {
                // This shared object already has been disposed of.
                if (this.disposedByPool) {
                    // If it has been disposed by the pool when the complete pool has been disposed, and now a client
                    // attemps to dispose of the object again, just return: we have nothing to do (this shared object
                    // already has been disposed of), and it is not an error, but a possible normal case during shutdown
                    // of the application.
                    return;
                } else {
                    // On the other hand, if this shared object has been disposed of by a client, and now a client
                    // attempts to dispose of this shared object again, it indicates an error.
                    throw new IllegalStateException("Method dispose() called on already disposed dynamic shared object");
                }
            }

            try {
                this.disposeCallback.run();
            } catch (Exception ex) {
                logger.error("Exception when calling dispose() on dynamic shared object, pooled object: "
                        + this.pooledObject);
            }

            // Mark this invocation handler as disposed, and allow to GC the shared object proxy.
            this.sharedObject = null;
        } finally {
            writeLock.unlock();
        }
    }


    private void disposeByPool() {
        // The shared object pool commands to dispose of this shared object.
        // Using writeLock (exclusive lock) to prevent parallel lifecycle events and/or usage of this shared object.
        Lock writeLock = this.lock.writeLock();
        writeLock.lock();
        try {
            if (this.sharedObject == null) {
                // This shared object already has been disposed of.
                if (this.disposedByPool) {
                    throw new IllegalStateException("Pool attempts to dispose of a dynamic shared object already disposed of by the pool");
                } else {
                    throw new IllegalStateException("Pool attempts to dispose of an already disposed dynamic shared object");
                }
            }

            // When disposing by the pool, do not call the dispose callback: the pool disposes of the shared object
            // itself, and we do not need to notify it back.
            //
            // Mark this invocation handler as disposed, and allow to GC the shared object proxy.
            this.sharedObject = null;
            this.disposedByPool = true;
        } finally {
            writeLock.unlock();
        }
    }


    private boolean isDisposed() {
        // Using readLock (shared lock): many threads may check the disposed status of this shared object
        // simultaneously.
        Lock readLock = this.lock.readLock();
        readLock.lock();
        try {
            return (this.sharedObject == null);
        } finally {
            readLock.unlock();
        }
    }


    private void setSharedObject(Object sharedObject) {
        // Using writeLock (exclusive lock) to prevent parallel lifecycle events and/or usage of this shared object.
        Lock writeLock = this.lock.writeLock();
        writeLock.lock();
        try {
            this.sharedObject = sharedObject;
        } finally {
            writeLock.unlock();
        }
    }


    @SuppressWarnings("unchecked")
    private static <S extends SharedObject, P> S create(Class<? extends S> type, P pooledObject, Runnable disposeCallback) {
        LockingSharedObject invocationHandler = new LockingSharedObject(pooledObject, disposeCallback);
        S sharedObject = (S) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, invocationHandler);
        invocationHandler.setSharedObject(sharedObject);
        return sharedObject;
    }


    /**
     * Returns a factory for creating shared objects with the specified type.
     *
     * @param <S> the type of shared objects to be created by the returned factory.
     * @param <P> the type of pooled objects required by the returned factory.
     *
     * @param type the type of shared objects to be created by the returned factory.
     *
     * @return a factory for creating shared objects with the specified type.
     */
    public static <S extends SharedObject, P> SharedObjectFactory<P, S> factory(Class<? extends S> type) {
        return new SharedObjectFactory<P, S>() {
            @Override
            public S createShared(P pooledObject, Runnable disposeCallback) {
                return LockingSharedObject.create(type, pooledObject, disposeCallback);
            }


            @Override
            public void disposeByPool(S sharedObject) {
                if (Proxy.isProxyClass(sharedObject.getClass())) {
                 Object invocationHandler = Proxy.getInvocationHandler(sharedObject);
                    ((LockingSharedObject) invocationHandler).disposeByPool();
                } else {
                    ((LockingSharedObject) sharedObject).disposeByPool();
                }
            }
        };
    }
}
