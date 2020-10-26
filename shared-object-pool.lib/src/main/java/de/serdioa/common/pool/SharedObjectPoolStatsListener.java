package de.serdioa.common.pool;

import java.util.EventListener;


public interface SharedObjectPoolStatsListener extends EventListener {

    /**
     * Invoked when the method {@link SharedObjectPool#get(Object)} is called.
     *
     * @param durationNanos duration of the method {@link SharedObjectPool#get(Object)} in nanoseconds.
     */
    void onSharedObjectGet(long durationNanos);


    /**
     * Invoked when a new pooled object is created.
     *
     * @param durationNanos duration in nanoseconds how long it took to create a new pooled object.
     */
    void onPooledObjectCreated(long durationNanos);


    /**
     * Invoked when a new pooled object is initialized.
     *
     * @param durationNanos duration in nanoseconds how long it took to initialize a new pooled object.
     */
    void onPooledObjectInitialized(long durationNanos);


    /**
     * Invoked when a pooled object is disposed of.
     *
     * @param durationNanos duration in nanoseconds how long it took to dispose of a pooled object.
     */
    void onPooledObjectDisposed(long durationNanos);
}
