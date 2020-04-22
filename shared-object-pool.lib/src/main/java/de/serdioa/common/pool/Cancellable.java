package de.serdioa.common.pool;


/**
 * A task which may be cancelled.
 */
public interface Cancellable {
    /**
     * Attempt to cancel this task. The attempt may fail, for example if the task is already executed.
     *
     * @return {@link true} if this task has been cancelled, {@code false} otherwise.
     */
    public boolean cancel();
}
