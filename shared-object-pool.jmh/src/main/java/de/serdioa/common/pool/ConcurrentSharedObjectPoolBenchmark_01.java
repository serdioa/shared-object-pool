package de.serdioa.common.pool;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import de.serdioa.common.pool.sample.PooledCounter;
import de.serdioa.common.pool.sample.PooledCounterFactory;
import de.serdioa.common.pool.sample.SharedCounter;
import de.serdioa.common.pool.sample.SharedCounterFactory;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
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
 * Benchmark for calling methods of shared objects.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class ConcurrentSharedObjectPoolBenchmark_01 {
    private ConcurrentSharedObjectPool<String, SharedCounter, PooledCounter> pool;

    @Param({"true"})
    private boolean disposeUnusedEntries;

    private AtomicLong index;

    @Setup(Level.Iteration)
    public void setup() {
        this.index = new AtomicLong();

        this.pool = new ConcurrentSharedObjectPool<>();
        this.pool.setPooledObjectFactory(new PooledCounterFactory());
        this.pool.setSharedObjectFactory(new SharedCounterFactory());
        this.pool.setDisposeUnusedEntries(this.disposeUnusedEntries);
    }


    @TearDown(Level.Iteration)
    public void tearDown() {
        this.index = null;

        this.pool.dispose();
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
    public int measureGetUniqueKey() {
        long i = this.index.getAndIncrement();

        SharedCounter counter = this.pool.get(String.valueOf(i));
        int value = counter.get();
        counter.dispose();
        return value;
    }


    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ConcurrentSharedObjectPoolBenchmark_01.class.getSimpleName())
                .warmupIterations(5)
                .warmupTime(TimeValue.seconds(5))
                .measurementIterations(10)
                .measurementTime(TimeValue.seconds(5))
                .forks(1)
                .threads(4)
                .syncIterations(true) // try to switch to "false"
                .build();

        new Runner(opt).run();
    }
}
