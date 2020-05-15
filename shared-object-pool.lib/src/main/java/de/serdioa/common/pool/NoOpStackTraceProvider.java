package de.serdioa.common.pool;


/**
 * A no-op implementation of the {@link StackTraceProvider} which always provides an empty stack trace. This implementation
 * may be used when tracking of shared objects which were not properly disposed of is disabled.
 */
public class NoOpStackTraceProvider implements StackTraceProvider {

    // The singleton empty array.
    private static final StackTraceElement [] EMPTY = new StackTraceElement[0];

    @Override
    public StackTraceElement[] provide() {
        return EMPTY;
    }
}
