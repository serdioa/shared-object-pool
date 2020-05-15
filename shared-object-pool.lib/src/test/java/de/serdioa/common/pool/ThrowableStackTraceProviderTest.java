package de.serdioa.common.pool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;


public class ThrowableStackTraceProviderTest extends AbstractStackTraceProviderTest {

    @Override
    protected StackTraceProvider buildStackTraceProvider() {
        return new ThrowableStackTraceProvider();
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
        StackTrace stackTrace = this.test_5(0);
        StackTraceElement[] elements = stackTrace.getElements();

        // Expected:
        // * AbstractStackTraceProviderTest.test_1()
        // * AbstractStackTraceProviderTest.test_2()
        // * AbstractStackTraceProviderTest.test_3()
        // * AbstractStackTraceProviderTest.test_4()
        // * AbstractStackTraceProviderTest.test_5()
        // * this method,
        // * the rest depends on JUnit framework and is not tested.
        assertTrue(elements.length > 5);
        for (int i = 0; i < 5; ++i) {
            assertEquals(this.getClass().getSuperclass().getName(), elements[i].getClassName());
            assertEquals("test_" + (i + 1), elements[i].getMethodName());
        }
        assertEquals(this.getClass().getName(), elements[5].getClassName());
        assertEquals("testProvideDeep", elements[5].getMethodName());
    }


    @Test
    public void testProvideDeepSkip() {
        StackTrace stackTrace = this.test_5(2);
        StackTraceElement[] elements = stackTrace.getElements();

        // Expected:
        // * AbstractStackTraceProviderTest.test_1() - skipped due to argument skipFrames = 2
        // * AbstractStackTraceProviderTest.test_2() - skipped due to argument skipFrames = 2
        // * AbstractStackTraceProviderTest.test_3()
        // * AbstractStackTraceProviderTest.test_4()
        // * AbstractStackTraceProviderTest.test_5()
        // * this method,
        // * the rest depends on JUnit framework and is not tested.
        assertTrue(elements.length > 3);
        for (int i = 0; i < 3; ++i) {
            assertEquals(this.getClass().getSuperclass().getName(), elements[i].getClassName());
            assertEquals("test_" + (i + 3), elements[i].getMethodName());
        }
        assertEquals(this.getClass().getName(), elements[3].getClassName());
        assertEquals("testProvideDeepSkip", elements[3].getMethodName());
    }
}
