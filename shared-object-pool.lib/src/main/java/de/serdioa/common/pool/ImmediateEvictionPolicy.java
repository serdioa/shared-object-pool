package de.serdioa.common.pool;


/**
 * Evict pooled objects immediately in the caller thread.
 */
public class ImmediateEvictionPolicy implements EvictionPolicy {

    // A no-op cancellable which can not cancel the task.
    private static final Cancellable NO_OP_CANCELLLABLE = () -> {
        return false;
    };


    @Override
    public Cancellable evict(Runnable evictionCallback) {
        evictionCallback.run();
        return NO_OP_CANCELLLABLE;
    }


    @Override
    public void dispose() {
        // No-op.
    }
}
