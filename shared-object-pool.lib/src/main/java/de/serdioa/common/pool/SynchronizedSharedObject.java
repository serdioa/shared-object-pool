package de.serdioa.common.pool;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Generic reflection-based implementation of {@link SharedObject} using synchronization.
 */
public class SynchronizedSharedObject implements InvocationHandler {

    private static final Logger logger = LoggerFactory.getLogger(SynchronizedSharedObject.class);

    private static final String DISPOSE_METHOD_NAME = "dispose";
    private static final String IS_DISPOSED_METHOD_NAME = "isDisposed";

    // @GuardedBy(this.mutex)
    private Object pooledObject;
    private final Runnable disposeCallback;

    // Synchronization lock for the lifecycle and accessing the pooled object.
    private final Object mutex = new Object();


    public SynchronizedSharedObject(Object pooledObject, Runnable disposeCallback) {
        // Use the lock to ensure proper visibility of this.pooledObject.
        synchronized (this.mutex) {
            this.pooledObject = Objects.requireNonNull(pooledObject);
            this.disposeCallback = Objects.requireNonNull(disposeCallback);
        }
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
        synchronized (this.mutex) {
            if (this.pooledObject == null) {
                throw new IllegalStateException("Method called on disposed dynamic shared object: " + method);
            }

            return method.invoke(this.pooledObject, args);
        }
    }


    private void dispose() {
        synchronized (this.mutex) {
            if (this.pooledObject == null) {
                throw new IllegalStateException("Method dispose() called on already disposed dynamic shared object");
            }

            try {
                this.disposeCallback.run();
            } catch (Exception ex) {
                logger.error("Exception when calling dispose() on dynamic shared object, pooled object: "
                        + this.pooledObject);
            }

            this.pooledObject = null;
        }
    }


    private boolean isDisposed() {
        synchronized (this.mutex) {
            return (this.pooledObject == null);
        }
    }


    @SuppressWarnings("unchecked")
    private static <S extends SharedObject, P> S create(Class<? extends S> type, P pooledObject, Runnable disposeCallback) {
        SynchronizedSharedObject invocationHandler = new SynchronizedSharedObject(pooledObject, disposeCallback);
        return (S) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, invocationHandler);
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
        return (pooledObject, disposeCallback) -> SynchronizedSharedObject.create(type, pooledObject, disposeCallback);
    }
}
