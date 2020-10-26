package de.serdioa.common.pool;


/**
 * Statistics about the performance of a {@link SharedObjectPool}.
 */
public interface SharedObjectPoolStats {

    /**
     * Returns the number of pooled objects in this pool. This number may include pooled objects which are not providing
     * any shared objects and thus are candidates for an eviction.
     *
     * @return the number of pooled objects in this pool.
     */
    int getPooledObjectsCount();


    /**
     * Returns the number of pooled objects in this pool which do not provide any shared objects and are candidates for
     * an eviction.
     *
     * @return the number of pooled objects in this pool which do not provide any shared objects.
     */
    int getUnusedPooledObjectsCount();


    /**
     * Returns the number of shared objects provided by this pool.
     *
     * @return the number of shared objects provided by this pool.
     */
    int getSharedObjectsCount();


    /**
     * Returns the number of times the method {@link SharedObjectPool#get(Object)} was invoked. The number is usually a
     * sum of {@link #getHitCount()} and {@link #getMissCount()}, but implementations are free to not use
     * synchronization when calculating metrics, so the numbers may be slightly off.
     *
     * @return the number of times the method {@link SharedObjectPool#get(Object)} was invoked.
     */
    default long getGetCount() {
        return this.getHitCount() + this.getMissCount();
    }


    /**
     * Returns the number of times the method {@link SharedObjectPool#get(Object)} has returned a cached pooled object.
     *
     * @return the number of times the method {@link SharedObjectPool#get(Object)} has returned a cached pooled object.
     */
    long getHitCount();


    /**
     * Returns the number of times the method {@link SharedObjectPool#get(Object)} has returned a newly created pooled
     * object.
     *
     * @return the number of times the method {@link SharedObjectPool#get(Object)} has returned a newly created pooled
     * object.
     */
    long getMissCount();


    /**
     * Returns the number of times the method {@link SharedObjectPool#get(Object)} has attempted to create a new pooled
     * object. The number is usually a sum of {@link #getCreateSuccessCount()} and {@link #getCreateExceptionCount()},
     * but implementations are free to not use synchronization when calculating metrics, so the numbers may be slightly
     * off.
     *
     * @return the number of times the method {@link SharedObjectPool#get(Object)} has attempted to create a new pooled
     * object.
     */
    default long getCreateCount() {
        return this.getCreateSuccessCount() + this.getCreateExceptionCount();
    }


    /**
     * Returns the number of times the method {@link SharedObjectPool#get(Object)} has successfully created a new pooled
     * object.
     *
     * @return the number of times the method {@link SharedObjectPool#get(Object)} has successfully created a new pooled
     * object.
     */
    long getCreateSuccessCount();


    /**
     * Returns the number of times the method {@link SharedObjectPool#get(Object)} failed to create a new pooled object.
     *
     * @return the number of times the method {@link SharedObjectPool#get(Object)} failed to create a new pooled object.
     */
    long getCreateExceptionCount();


    /**
     * Returns a number of times this pool evicted unused objects. The number is usually a sum of
     * {@link #getEvictionSuccessCount()} and {@link #getEvictionExceptionCount()}, but implementations are free to not
     * use synchronization when calculating metrics, so the numbers may be slightly off.
     *
     * @return a number of times this pool evicted unused objects.
     */
    default long getEvictionCount() {
        return this.getEvictionSuccessCount() + this.getEvictionExceptionCount();
    }


    /**
     * Returns a number of times this pool successfully disposed of an evicted object.
     *
     * @return a number of times this pool successfully disposed of an evicted object.
     */
    long getEvictionSuccessCount();


    /**
     * Returns a number of times this pool failed to properly dispose of an evicted object. The object was still evicted
     * from the pool.
     *
     * @return a number of times this pool failed to properly dispose of an evicted object.
     */
    long getEvictionExceptionCount();


    /**
     * Adds a listener to be notified on events related to performance of a pool.
     *
     * @param listener the listener to be added.
     */
    void addSharedObjectPoolStatsListener(SharedObjectPoolStatsListener listener);


    /**
     * Removes a listener to be notified on events related to performance of a pool.
     *
     * @param listener the listener to be removed.
     */
    void removeSharedObjectPoolStatsListener(SharedObjectPoolStatsListener listener);
}
