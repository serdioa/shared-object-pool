package de.serdioa.common.pool;


/**
 * Indicates how a shared object has been disposed of. Tracking how a shared object has been disposed of allows to catch
 * errors, such as disposing of the same shared object twice.
 */
enum SharedObjectDisposeType {
    /**
     * The shared object has been disposed of directly by the client, calling the method {@link SharedObject#dispose()}
     * on the shared object in question.
     */
    DIRECT,

    /**
     * The shared object has been disposed of by the reaper, that is a component which monitors shared objects abandoned
     * by clients without properly dispose of them.
     */
    REAPER,

    /**
     * The shared object has been disposed of when the pool has been shut down.
     */
    SHUTDOWN
}
