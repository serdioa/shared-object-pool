package de.serdioa.common.pool;

import static org.junit.Assert.assertEquals;

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
}
