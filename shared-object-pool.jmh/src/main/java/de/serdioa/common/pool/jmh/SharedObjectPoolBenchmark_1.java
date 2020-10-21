package de.serdioa.common.pool.jmh;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;


/**
 * Baseline benchmark: invoke methods directly on pooled objects without constructing shared wrappers and going through
 * them.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class SharedObjectPoolBenchmark_1 {

    /**
     * The number of CPU time tokens to spend in each call of the test method.
     */
    @Param({"0", "10", "100"})
    public int tokens;

    /**
     * The number of pooled objects.
     */
    @Param({"1", "10", "100"})
    public int pooledObjectsCount;

    // Pooled objects used by tests.
    private PooledTestObject[] pooledObjects;


    @Setup
    public void setup() {
        this.pooledObjects = new PooledTestObject[this.pooledObjectsCount];
        for (int i = 0; i < this.pooledObjectsCount; ++i) {
            this.pooledObjects[i] = new PooledTestObject();
        }
    }


    @Benchmark
    public int rndBaseline() {
        return ThreadLocalRandom.current().nextInt(this.pooledObjectsCount);
    }


    @Benchmark
    public void pooledBaseline() {
        int index = ThreadLocalRandom.current().nextInt(this.pooledObjectsCount);
        PooledTestObject pooled = this.pooledObjects[index];
        pooled.run(this.tokens);
    }


    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SharedObjectPoolBenchmark_1.class.getSimpleName())
                .forks(1)
                .syncIterations(true)
                .build();

        new Runner(opt).run();
    }
}
