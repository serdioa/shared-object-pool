package de.serdioa.common.pool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;


public class SecurityManagerStackTraceProviderTest {

    private StackTraceProvider stackTraceProvider;


    @Before
    public void setUp() {
        this.stackTraceProvider = new SecurityManagerStackTraceProvider();
    }


    @Test
    public void testProvide() {
        StackTraceElement[] trace = this.stackTraceProvider.provide();

        assertTrue(trace.length > 0);
        assertEquals(this.getClass().getCanonicalName(), trace[0].getClassName());
        // Method name is not available from this stack trace provider.
    }
}
