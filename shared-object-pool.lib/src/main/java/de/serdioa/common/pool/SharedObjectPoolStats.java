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
