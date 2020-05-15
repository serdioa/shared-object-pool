package de.serdioa.common.pool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;


public class SecurityManagerStackTraceProviderTest extends AbstractStackTraceProviderTest {

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
        assertEquals(this.getClass().getName(), elements[0].getClassName());
        // Method name is not available from this stack trace provider.
    }


    @Test
    public void testProvideDeep() {
        StackTrace stackTrace = new Wrapper(this.stackTraceProvider::provide, 3).get();
        StackTraceElement[] elements = stackTrace.getElements();

        // Expected:
        // * top element from lambda expression provided as an argument to the Wrapper constructor,
        // * 3 frames are from Wrapper,
        // * 1 frame from this method,
        // * the rest depends on JUnit framework and is not tested.
        assertTrue(elements.length > 4);
        assertTrue(elements[0].getClassName().contains("Lambda"));
        assertEquals(Wrapper.class.getName(), elements[1].getClassName());
        assertEquals(Wrapper.class.getName(), elements[2].getClassName());
        assertEquals(Wrapper.class.getName(), elements[3].getClassName());
        assertEquals(this.getClass().getName(), elements[4].getClassName());
    }
}
