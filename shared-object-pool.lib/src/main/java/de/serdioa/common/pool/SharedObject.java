package de.serdioa.common.pool;


/**
 * A wrapper for a pooled object provided by a pool to clients. After a client is done using the shared object, the
 * client shall call the method {@link SharedObject#dispose()}, informing the pool that the shared object is not
 * required anymore. The pool may dispose of the underlying implementation when no client requires a particular pooled
 * object.
 */
public interface SharedObject {

    /**
     * Dispose of this shared object, informing the pool that the client does not require this shared object anymore.
     * After an object is disposed, calling on it any method except {@link #isDisposed()} may throw an exception.
     *
     * @see #isDisposed()
     */
    void dispose();


    /**
     * Checks if this shared object is already disposed of. After an object is disposed, calling on it any method except
     * {@link #isDisposed()} may throw an exception.
     *
     * @return {@code true} if this shared object is already disposed of, {@code false} otherwise.
     *
     * @see #dispose()
     */
    boolean isDisposed();
}
