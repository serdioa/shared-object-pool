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
        StackTraceElement[] trace = this.stackTraceProvider.provide();

        assertTrue(trace.length > 0);
        assertEquals(this.getClass().getName(), trace[0].getClassName());
        assertEquals("testProvide", trace[0].getMethodName());
    }
}
