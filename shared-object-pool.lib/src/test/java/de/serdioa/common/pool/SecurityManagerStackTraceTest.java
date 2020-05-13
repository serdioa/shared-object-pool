package de.serdioa.common.pool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;


public class SecurityManagerStackTraceTest {

    private StackTrace stackTrace;


    @Before
    public void setUp() {
        this.stackTrace = new SecurityManagerStackTrace();
    }


    @Test
    public void testProvide() {
        StackTraceElement[] trace = this.stackTrace.provide();

        assertTrue(trace.length > 0);
        assertEquals(this.getClass().getCanonicalName(), trace[0].getClassName());
        // Method name is not available from this stack trace provider.
    }
}
