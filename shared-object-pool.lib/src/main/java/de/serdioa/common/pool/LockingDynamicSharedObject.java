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


public class LockingDynamicSharedObject implements InvocationHandler {
    private static final Logger logger = LoggerFactory.getLogger(LockingDynamicSharedObject.class);

    private static final String DISPOSE_METHOD_NAME = "dispose";
    private static final String IS_DISPOSED_METHOD_NAME = "isDisposed";

    private PooledObject pooledObject;
    private final Runnable disposeCallback;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public LockingDynamicSharedObject(PooledObject pooledObject, Runnable disposeCallback) {
        this.pooledObject = Objects.requireNonNull(pooledObject);
        this.disposeCallback = Objects.requireNonNull(disposeCallback);
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Special methods.
        if (args == null || args.length == 0) {
            if (DISPOSE_METHOD_NAME.equals(method.getName())) {
                this.dispose();
                return null;
            } else if (IS_DISPOSED_METHOD_NAME.equals(method.getName())) {
                return this.isDisposed();
            }
        }

        // All other methods are forwarded to the pooled object.
        return invokePooled(method, args);
    }


    private void dispose() {
        // Dispose of this shared object.
        Lock writeLock = this.lock.writeLock();
        writeLock.lock();
        try {
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
        } finally {
            writeLock.unlock();
        }
    }


    private Object invokePooled(Method method, Object[] args) throws Throwable {
        Lock readLock = this.lock.readLock();
        readLock.lock();
        try {
            if (this.pooledObject == null) {
                throw new IllegalStateException("Method called on disposed dynamic shared object: " + method);
            }

            return method.invoke(this.pooledObject, args);
        } finally {
            readLock.unlock();
        }
    }


    private boolean isDisposed() {
        Lock readLock = this.lock.readLock();
        readLock.lock();
        try {
            return (this.pooledObject == null);
        } finally {
            readLock.unlock();
        }
    }




    @SuppressWarnings("unchecked")
    public static <S extends SharedObject, P extends PooledObject> S create(Class<S> type, P pooledObject, Runnable disposeCallback) {
        LockingDynamicSharedObject invocationHandler = new LockingDynamicSharedObject(pooledObject, disposeCallback);
        return (S) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, invocationHandler);
    }
}
