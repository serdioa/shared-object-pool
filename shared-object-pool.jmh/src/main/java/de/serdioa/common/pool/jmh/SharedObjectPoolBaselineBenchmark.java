package de.serdioa.common.pool.jmh;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import de.serdioa.common.pool.LockingSharedObject;
import de.serdioa.common.pool.SharedObjectFactory;
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
 * Baseline benchmark: compare various methods of constructing pooled and shared objects.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class SharedObjectPoolBaselineBenchmark {

    /**
     * A benchmark state for tests which rely on pre-created pooled and/or shared objects.
     */
    @State(Scope.Benchmark)
    public static class PreCreatedState {

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

        // Shared objects used by tests.
        private SharedTestObject[] sharedObjects;


        @Setup
        public void setup() {
            this.pooledObjects = new PooledTestObject[this.pooledObjectsCount];
            this.sharedObjects = new SharedTestObject[this.pooledObjectsCount];

            for (int i = 0; i < this.pooledObjectsCount; ++i) {
                this.pooledObjects[i] = new PooledTestObject();
                this.sharedObjects[i] = LockingSharedObject.factory(SharedTestObject.class).createShared(
                        this.pooledObjects[i], () -> {
                        });
            }
        }
    }


    /**
     * A benchmark state for tests which do rely on pre-created pooled and/or shared objects, but still require a
     * minimum configuration.
     */
    @State(Scope.Benchmark)
    public static class SimpleState {

        /**
         * The number of CPU time tokens to spend in each call of the test method.
         */
        @Param({"0", "10", "100"})
        public int tokens;
    }


    /**
     * Baseline overhead of using a random number generator.
     */
    @Benchmark
    public int rndBaseline(PreCreatedState state) {
        return ThreadLocalRandom.current().nextInt(state.pooledObjectsCount);
    }


    /**
     * Test method invoked directly on pre-created pooled objects.
     */
    @Benchmark
    public void preCreatedPooled(PreCreatedState state) {
        int index = ThreadLocalRandom.current().nextInt(state.pooledObjectsCount);
        PooledTestObject pooled = state.pooledObjects[index];
        pooled.run(state.tokens);
    }


    /**
     * Test method invoked directly on newly created pooled objects.
     */
    @Benchmark
    public void creatingPooled(SimpleState state) {
        PooledTestObject pooled = new PooledTestObject();
        pooled.run(state.tokens);
    }


    /**
     * Test method invoked on pre-created shared object wrappers.
     */
    @Benchmark
    public void preCreatedShared(PreCreatedState state) {
        int index = ThreadLocalRandom.current().nextInt(state.pooledObjectsCount);
        SharedTestObject shared = state.sharedObjects[index];
        shared.run(state.tokens);
    }


    /**
     * Test method invoked on newly created shared object wrappers based on pre-created pooled objects.
     */
    @Benchmark
    public void creatingShared(PreCreatedState state) {
        int index = ThreadLocalRandom.current().nextInt(state.pooledObjectsCount);
        PooledTestObject pooled = state.pooledObjects[index];
        SharedTestObject shared = LockingSharedObject.factory(SharedTestObject.class).createShared(pooled, () -> {
        });

        shared.run(state.tokens);
    }


    /**
     * Test method invoked on newly created shared object wrappers based on newly created pooled objects.
     */
    @Benchmark
    public void creatingPooledAndShared(SimpleState state) {
        PooledTestObject pooled = new PooledTestObject();
        SharedTestObject shared = LockingSharedObject.factory(SharedTestObject.class).createShared(pooled, () -> {
        });

        shared.run(state.tokens);
    }


    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SharedObjectPoolBaselineBenchmark.class.getSimpleName())
                .forks(1)
                .syncIterations(true)
                .build();

        new Runner(opt).run();
    }
}
