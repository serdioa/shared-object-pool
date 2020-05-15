package de.serdioa.common.pool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;


public class NoOpStackTraceProviderTest {

    private StackTraceProvider stackTraceProvider;


    @Before
    public void setUp() {
        this.stackTraceProvider = new NoOpStackTraceProvider();
    }


    @Test
    public void testProvide() {
        StackTrace stackTrace = this.stackTraceProvider.provide();
        StackTraceElement[] elements = stackTrace.getElements();
        assertEquals(0, elements.length);
    }


    @Test
    public void testProvideDeep() {
        StackTrace stackTrace = new AbstractStackTraceProviderTest.Wrapper(this.stackTraceProvider::provide, 3).get();
        StackTraceElement[] elements = stackTrace.getElements();

        // No-op provider always returns an empty stack trace.
        assertEquals(0, elements.length);
    }
}
