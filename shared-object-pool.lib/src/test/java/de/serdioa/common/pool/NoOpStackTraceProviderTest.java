package de.serdioa.common.pool;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class NoOpStackTraceProviderTest extends AbstractStackTraceProviderTest {

    @Override
    protected StackTraceProvider buildStackTraceProvider() {
        return new NoOpStackTraceProvider();
    }


    @Test
    public void testProvide() {
        StackTrace stackTrace = this.stackTraceProvider.provide();
        StackTraceElement[] elements = stackTrace.getElements();
        assertEquals(0, elements.length);
    }


    @Test
    public void testProvideDeep() {
        StackTrace stackTrace = this.test_5(0);
        StackTraceElement[] elements = stackTrace.getElements();

        // No-op provider always returns an empty stack trace.
        assertEquals(0, elements.length);
    }


    @Test
    public void tetProvideDeepSkip() {
        StackTrace stackTrace = this.test_5(2);
        StackTraceElement[] elements = stackTrace.getElements();

        // No-op provider always returns an empty stack trace.
        assertEquals(0, elements.length);
    }
}
