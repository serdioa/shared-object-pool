package de.serdioa.common.pool;


/**
 * Provides the current call stack for tracking shared objects which were not properly disposed of.
 */
public interface StackTrace {
    /**
     * Returns the current call stack as an array of stack trace elements. The returned array may be empty,
     * if a particular implementation is optimized for speed instead of precision.
     * 
     * @return an array of stack trace elements representing the current call stack.
     */
    StackTraceElement [] provide();
}
