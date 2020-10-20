package de.serdioa.common.pool.jmh;

import java.util.concurrent.TimeUnit;

import de.serdioa.common.pool.LockingSharedObject;
import de.serdioa.common.pool.SynchronizedSharedObject;
import de.serdioa.common.pool.sample.Counter;
import de.serdioa.common.pool.sample.LockingSharedCounter;
import de.serdioa.common.pool.sample.PooledCounter;
import de.serdioa.common.pool.sample.SharedCounter;
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


/**
 * A benchmark for various implementations of a shared counter.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class SharedCounterBenchmark {

    // Abstract base class for the test state.
    public static abstract class AbstractState {

        @Param({"pooled", "locking", "sync", "reflection-locking", "reflection-sync"})
        public String type;


        protected Counter buildSharedCounter(PooledCounter pooledCounter) {
            switch (this.type) {
                case "pooled":
                    return pooledCounter;
                case "sync":
                    return new SynchronizedSharedCounter(pooledCounter, () -> {
                    });
                case "reflection-sync":
                    return SynchronizedSharedObject.factory(SharedCounter.class).createShared(pooledCounter, () -> {
                    });
                case "locking":
                    return new LockingSharedCounter(pooledCounter, () -> {
                    });
                case "reflection-locking":
                    return LockingSharedObject.factory(SharedCounter.class).createShared(pooledCounter, () -> {
                    });
                default:
                    throw new IllegalArgumentException("Unexpected type of the shared counter: " + this.type);
            }
        }


        public void disposeSharedCounter(Counter sharedCounter) {
            if (sharedCounter instanceof SharedCounter) {
                ((SharedCounter) sharedCounter).dispose();
            }
        }
    }


    // Test state where both pooled and shared object are shared by all threads.
    @State(Scope.Benchmark)
    public static class SharedState extends AbstractState {

        private PooledCounter pooled;
        private Counter shared;


        @Setup
        public void setup() {
            this.pooled = new PooledCounter("AAA");
            this.pooled.initialize();

            this.shared = buildSharedCounter(this.pooled);
        }


        @TearDown
        public void tearDown() {
            this.disposeSharedCounter(this.shared);
            this.shared = null;

            this.pooled.dispose();
            this.pooled = null;
        }
    }


    // Test state where both pooled and shared object are specific to each thread.
    @State(Scope.Thread)
    public static class ThreadState extends AbstractState {

        private PooledCounter pooled;
        private Counter shared;


        @Setup
        public void setup() {
            this.pooled = new PooledCounter("AAA");
            this.pooled.initialize();

            this.shared = buildSharedCounter(this.pooled);
        }


        @TearDown
        public void tearDown() {
            this.disposeSharedCounter(this.shared);
            this.shared = null;

            this.pooled.dispose();
            this.pooled = null;
        }
    }


    // Test state where pooled object is shared between threads, but shared object is specific to each thread.
    @State(Scope.Thread)
    public static class MixedState extends AbstractState {

        // The test pooled counter shared by all threads.
        // During the initialization, access to the pooled counter is guarded by the pooledLock.
        private static PooledCounter pooled;
        private static final Object pooledLock = new Object();

        private Counter shared;


        @Setup
        public void setup() {
            PooledCounter pooledSnapshot;
            synchronized (MixedState.pooledLock) {
                // The first thread in the synchronization block initializes the pooled counter.
                if (MixedState.pooled == null) {
                    MixedState.pooled = new PooledCounter("AAA");
                    MixedState.pooled.initialize();
                }
                pooledSnapshot = MixedState.pooled;
            }

            this.shared = buildSharedCounter(this.pooled);
        }


        @TearDown
        public void tearDown() {
            this.disposeSharedCounter(this.shared);
            this.shared = null;

            synchronized (this.pooledLock) {
                // The first thread in the synchronization block disposes of the pooled counter.
                if (MixedState.pooled == null) {
                    MixedState.pooled.dispose();
                    MixedState.pooled = null;
                }
            }
        }
    }


    @Benchmark
    public int getShared(SharedState state) {
        return state.pooled.get();
    }


    @Benchmark
    public int incrementShared(SharedState state) {
        return state.pooled.increment();
    }


    @Benchmark
    public int getThread(ThreadState state) {
        return state.pooled.get();
    }


    @Benchmark
    public int incrementThread(ThreadState state) {
        return state.pooled.increment();
    }


    @Benchmark
    public int getMixed(MixedState state) {
        return state.pooled.get();
    }


    @Benchmark
    public int incrementMixed(MixedState state) {
        return state.pooled.increment();
    }


    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SharedCounterBenchmark.class.getSimpleName())
                .forks(1)
                .syncIterations(true)
                .build();

        new Runner(opt).run();
    }
}
