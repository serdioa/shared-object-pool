package de.serdioa.common.pool;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;


public class NoOpStackTraceTest {

    private StackTrace stackTrace;


    @Before
    public void setUp() {
        this.stackTrace = new NoOpStackTrace();
    }


    @Test
    public void testProvide() {
        StackTraceElement[] trace = this.stackTrace.provide();
        assertEquals(0, trace.length);
    }
}
