package de.serdioa.common.pool;

import java.util.Arrays;


/**
 * An implementation of the {@link StackTrace} which uses a {@link Throwable} to obtain the current call stack. This
 * implementation is the slowest, but the most detailed. It may provide even line numbers, if the code was compiled with
 * the required information.
 */
public class ThrowableStackTrace implements StackTrace {

    @Override
    public StackTraceElement[] provide() {
        // Create an instance of Throwable and get the call stack.
        Throwable t = new Throwable();
        StackTraceElement[] throwableStackTrace = t.getStackTrace();

        // Remove the top element of the call stack, which is this method, so that the caller gets only call stack
        // up to him.
        return Arrays.copyOfRange(throwableStackTrace, 1, throwableStackTrace.length);
    }

}
