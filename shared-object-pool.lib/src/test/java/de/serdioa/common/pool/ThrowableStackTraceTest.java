package de.serdioa.common.pool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;


public class ThrowableStackTraceTest {

    private StackTrace stackTrace;


    @Before
    public void setUp() {
        this.stackTrace = new ThrowableStackTrace();
    }


    @Test
    public void testProvide() {
        StackTraceElement[] trace = this.stackTrace.provide();

        assertTrue(trace.length > 0);
        assertEquals(this.getClass().getName(), trace[0].getClassName());
        assertEquals("testProvide", trace[0].getMethodName());
    }
}
