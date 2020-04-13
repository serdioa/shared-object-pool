package de.serdioa.common.pool;

import java.util.concurrent.TimeUnit;

import de.serdioa.common.pool.sample.Counter;
import de.serdioa.common.pool.sample.PooledCounter;
import de.serdioa.common.pool.sample.LockingSharedCounter;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;


@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class DynamicSharedObjectBenchmark {
    private interface DynamicSharedCounter extends Counter, SharedObject {}

    private PooledCounter pooledCounter;
    private LockingSharedCounter sharedCounter;
    private DynamicSharedCounter lockingDynamicSharedCounter;


    @Setup
    public void setup() {
        this.pooledCounter = new PooledCounter("AAA");
        this.pooledCounter.initialize();

        this.sharedCounter = new LockingSharedCounter(this.pooledCounter, () -> {});

        this.lockingDynamicSharedCounter = LockingSharedObject.factory(DynamicSharedCounter.class)
                .createShared(this.pooledCounter, () -> {});
    }


    @TearDown
    public void tearDown() {
        this.lockingDynamicSharedCounter = null;
        this.sharedCounter = null;

        this.pooledCounter.dispose();
        this.pooledCounter = null;
    }


    @Benchmark
    public int testPooledCounter() {
        return this.pooledCounter.get();
    }


    @Benchmark
    public int testSharedCounter() {
        return this.sharedCounter.get();
    }


    @Benchmark
    public int testLockingDynamicSharedCounter() {
        return this.lockingDynamicSharedCounter.get();
    }



    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(DynamicSharedObjectBenchmark.class.getSimpleName())
                .warmupIterations(5)
                .warmupTime(TimeValue.seconds(5))
                .measurementIterations(10)
                .measurementTime(TimeValue.seconds(5))
                .forks(1)
                .syncIterations(true) // try to switch to "false"
                .build();

        new Runner(opt).run();
    }
}
