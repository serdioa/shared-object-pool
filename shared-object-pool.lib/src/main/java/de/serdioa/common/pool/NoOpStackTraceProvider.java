package de.serdioa.common.pool;


/**
 * A no-op implementation of the {@link StackTraceProvider} which always provides an empty stack trace. This
 * implementation may be used when tracking of shared objects which were not properly disposed of is disabled.
 */
public class NoOpStackTraceProvider implements StackTraceProvider {

    @Override
    public StackTrace provide() {
        return StackTrace.empty();
    }


    @Override
    public StackTrace provide(int skipFrames) {
        return StackTrace.empty();
    }
}
