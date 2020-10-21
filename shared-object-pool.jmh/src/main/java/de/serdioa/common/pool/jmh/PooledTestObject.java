package de.serdioa.common.pool.jmh;

import org.openjdk.jmh.infra.Blackhole;


/**
 * The implementation of the object used for performance benchmarks.
 */
public class PooledTestObject implements TestObject {

    @Override
    public void run(int tokens) {
        Blackhole.consumeCPU(tokens);
    }
}
