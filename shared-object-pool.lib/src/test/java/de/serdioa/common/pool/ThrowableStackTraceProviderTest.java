package de.serdioa.common.pool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;


public class ThrowableStackTraceProviderTest {

    private StackTraceProvider stackTraceProvider;


    @Before
    public void setUp() {
        this.stackTraceProvider = new ThrowableStackTraceProvider();
    }


    @Test
    public void testProvide() {
        StackTrace stackTrace = this.stackTraceProvider.provide();
        StackTraceElement[] elements = stackTrace.getElements();

        assertTrue(elements.length > 0);
        assertEquals(this.getClass().getName(), elements[0].getClassName());
        assertEquals("testProvide", elements[0].getMethodName());
    }
}
