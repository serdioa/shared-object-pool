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
        StackTrace stackTrace = this.stackTraceProvider.provide();
        StackTraceElement[] elements = stackTrace.getElements();

        assertTrue(elements.length > 0);
        assertEquals(this.getClass().getCanonicalName(), elements[0].getClassName());
        // Method name is not available from this stack trace provider.
    }
}
