package de.serdioa.common.pool;


/**
 * Provides the current call stack for tracking shared objects which were not properly disposed of.
 */
public interface StackTraceProvider {

    /**
     * Returns the current call stack. The returned call stack may be empty, if a particular implementation is optimized
     * for speed instead of precision.
     *
     * @return a stack trace representing the current call stack.
     */
    StackTrace provide();


    /**
     * Returns the current call stack with the specified number of frames on top skipped. The returned call stack may be
     * empty, if a particular implementation is optimized for speed instead of precision.
     *
     * @param skipFrames the number of frames on top to be skipped.
     *
     * @return a stack trace representing the current call stack, with the specified number of frames on top skipped.
     */
    StackTrace provide(int skipFrames);
}
