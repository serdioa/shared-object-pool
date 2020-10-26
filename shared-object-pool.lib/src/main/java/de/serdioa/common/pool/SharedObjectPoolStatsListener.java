package de.serdioa.common.pool;

import java.util.EventListener;


public interface SharedObjectPoolStatsListener extends EventListener {

    /**
     * Invoked when the method {@link SharedObjectPool#get(Object)} is called.
     *
     * @param durationNanos duration of the method {@link SharedObjectPool#get(Object)} in nanoseconds.
     * @param hit {@code true} if the method {@link SharedObjectPool#get(Object)} returned an already existing pooled
     * object, {@code false} if it returned an newly created pooled object.
     */
    void onSharedObjectGet(long durationNanos, boolean hit);


    /**
     * Invoked when a new pooled object is created.
     *
     * @param durationNanos duration in nanoseconds how long it took to create a new pooled object.
     * @param success {@code true} if the new pooled object was created successfully, {@code false} if an attempt to
     * create a new pooled object throws an exception.
     */
    void onPooledObjectCreated(long durationNanos, boolean success);


    /**
     * Invoked when a new pooled object is initialized.
     *
     * @param durationNanos duration in nanoseconds how long it took to initialize a new pooled object.
     * @param success {@code true} if the new pooled object was initialized successfully, {@code false} if an attempt to
     * initialize a new pooled object throws an exception.
     */
    void onPooledObjectInitialized(long durationNanos, boolean success);


    /**
     * Invoked when a pooled object is disposed of.
     *
     * @param durationNanos duration in nanoseconds how long it took to dispose of a pooled object.
     * @param success {@code true} if the pooled object has been disposed of successfully, {@code false} if an attempt
     * to dispose of a pooled object throws an exception.
     */
    void onPooledObjectDisposed(long durationNanos, boolean success);
}
