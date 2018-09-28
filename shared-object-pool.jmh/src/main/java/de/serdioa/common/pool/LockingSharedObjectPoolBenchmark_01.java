package de.serdioa.common.pool;

import java.util.concurrent.TimeUnit;

import de.serdioa.common.pool.sample.PooledCounter;
import de.serdioa.common.pool.sample.PooledCounterFactory;
import de.serdioa.common.pool.sample.SharedCounter;
import de.serdioa.common.pool.sample.SharedCounterFactory;
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


/**
 * Separate benchmarks for get() and increment().
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class LockingSharedObjectPoolBenchmark_01 {
    private LockingSharedObjectPool<String, SharedCounter, PooledCounter> pool;

    @Setup
    public void setup() {
        this.pool = new LockingSharedObjectPool<>();
        this.pool.setPooledObjectFactory(new PooledCounterFactory());
        this.pool.setSharedObjectFactory(new SharedCounterFactory());
    }


    @TearDown
    public void tearDown() {
        this.pool = null;
    }


    @Benchmark
    public int measureGet() {
        SharedCounter counter = this.pool.get("AAA");
        int value = counter.get();
        counter.dispose();
        return value;
    }


    @Benchmark
    public int measureIncrement() {
        SharedCounter counter = this.pool.get("AAA");
        int value = counter.increment();
        counter.dispose();
        return value;
    }


    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(LockingSharedObjectPoolBenchmark_01.class.getSimpleName())
                .warmupTime(TimeValue.seconds(5))
                .measurementTime(TimeValue.seconds(10))
                .syncIterations(true) // try to switch to "false"
                .build();

        new Runner(opt).run();
    }
}
