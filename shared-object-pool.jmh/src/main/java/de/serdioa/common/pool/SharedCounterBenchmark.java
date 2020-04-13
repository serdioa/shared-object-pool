package de.serdioa.common.pool;

import java.util.concurrent.TimeUnit;

import de.serdioa.common.pool.sample.Counter;
import de.serdioa.common.pool.sample.LockingSharedCounter;
import de.serdioa.common.pool.sample.PooledCounter;
import de.serdioa.common.pool.sample.SynchronizedSharedCounter;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
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
 * A benchmark for various implementations of a shared counter.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class SharedCounterBenchmark {

    public static interface SharedCounter extends Counter, SharedObject {
    }


    public static abstract class AbstractState {

        @Param({"pooled", "locking", "sync", "reflection-locking", "reflection-sync"})
        public String type;

        public PooledCounter pooledCounter;
        public Counter counter;


        @Setup
        public void setup() {
            this.pooledCounter = new PooledCounter("AAA");
            this.pooledCounter.initialize();

            switch (this.type) {
                case "pooled":
                    this.counter = this.pooledCounter;
                    break;
                case "locking":
                    this.counter = new LockingSharedCounter(this.pooledCounter, () -> {
                    });
                    break;
                case "sync":
                    this.counter = new SynchronizedSharedCounter(this.pooledCounter, () -> {
                    });
                    break;
                case "reflection-locking":
                    this.counter = LockingSharedObject.factory(SharedCounter.class).createShared(
                            this.pooledCounter, () -> {
                            });
                    break;
                case "reflection-sync":
                    this.counter = SynchronizedSharedObject.factory(SharedCounter.class).createShared(
                            this.pooledCounter, () -> {
                            });
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected type of the shared counter: " + this.type);
            }
        }


        @TearDown
        public void tearDown() {
            // this.counter and this.counter may be the same object, or they may be different objects.
            // We shall separately dispose of this.counter only if it is a separate shared object.
            if (this.counter instanceof SharedObject) {
                SharedObject sharedCounter = (SharedObject) this.counter;
                sharedCounter.dispose();
            }
            this.counter = null;

            this.pooledCounter.dispose();
            this.pooledCounter = null;
        }
    }


    @State(Scope.Benchmark)
    public static class SharedState extends AbstractState {
    }


    @State(Scope.Thread)
    public static class ThreadState extends AbstractState {
    }


    @Benchmark
    public int getThread(ThreadState state) {
        return state.counter.get();
    }


    @Benchmark
    public int incrementThread(ThreadState state) {
        return state.counter.increment();
    }


    @Benchmark
    public int getShared(SharedState state) {
        return state.counter.get();
    }


    @Benchmark
    public int incrementShared(SharedState state) {
        return state.counter.increment();
    }


    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SharedCounterBenchmark.class.getSimpleName())
                .warmupIterations(5)
                .warmupTime(TimeValue.seconds(5))
                .measurementIterations(10)
                .measurementTime(TimeValue.seconds(5))
                .forks(1)
                .syncIterations(true)
                .build();

        new Runner(opt).run();
    }
}
