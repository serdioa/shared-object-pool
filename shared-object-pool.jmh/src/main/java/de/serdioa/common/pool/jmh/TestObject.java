package de.serdioa.common.pool.jmh;

import org.openjdk.jmh.infra.Blackhole;


/**
 * A simple object for performance benchmarks. The object defines one method {@link #run()} which just consumes the
 * specified number of CPU time tokens.
 */
public interface TestObject {

    /**
     * A test method that just consumes the specified number of CPU time tokens.
     *
     * @param tokens the number of CPU time tokens to consume.
     *
     * @see Blackhole#consumeCPU(long)
     */
    void run(int tokens);
}
