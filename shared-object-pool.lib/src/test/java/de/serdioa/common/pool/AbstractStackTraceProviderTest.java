package de.serdioa.common.pool;

import org.junit.Before;


public abstract class AbstractStackTraceProviderTest {

    protected StackTraceProvider stackTraceProvider;


    @Before
    public void setUp() {
        this.stackTraceProvider = this.buildStackTraceProvider();
    }


    protected abstract StackTraceProvider buildStackTraceProvider();


    // Several helper function for testing multi-level stack traces.
    protected StackTrace test_1(int skipFrames) {
        return this.stackTraceProvider.provide(skipFrames);
    }


    protected StackTrace test_2(int skipFrames) {
        return this.test_1(skipFrames);
    }


    protected StackTrace test_3(int skipFrames) {
        return this.test_2(skipFrames);
    }


    protected StackTrace test_4(int skipFrames) {
        return this.test_3(skipFrames);
    }


    protected StackTrace test_5(int skipFrames) {
        return this.test_4(skipFrames);
    }
}
