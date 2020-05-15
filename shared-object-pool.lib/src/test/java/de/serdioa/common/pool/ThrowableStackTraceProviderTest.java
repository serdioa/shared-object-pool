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


    @Test
    public void testProvideDeep() {
        StackTrace stackTrace = new AbstractStackTraceProviderTest.Wrapper(this.stackTraceProvider::provide, 3).get();
        StackTraceElement[] elements = stackTrace.getElements();

        // Expected:
        // * 3 frames are from Wrapper,
        // * 1 frame from this method,
        // * the rest depends on JUnit framework and is not tested.
        //
        // Note that the ThrowableStackTraceProvider excludes lambda expressions from the stack trace,
        // the same as Throwable.printStackTrace().
        assertTrue(elements.length > 3);
        assertEquals(AbstractStackTraceProviderTest.Wrapper.class.getName(), elements[0].getClassName());
        assertEquals(AbstractStackTraceProviderTest.Wrapper.class.getName(), elements[1].getClassName());
        assertEquals(AbstractStackTraceProviderTest.Wrapper.class.getName(), elements[2].getClassName());
        assertEquals(this.getClass().getName(), elements[3].getClassName());
    }
}
