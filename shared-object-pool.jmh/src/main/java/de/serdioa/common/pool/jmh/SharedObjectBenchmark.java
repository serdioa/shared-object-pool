package de.serdioa.common.pool.jmh;

import java.util.concurrent.TimeUnit;

import de.serdioa.common.pool.LockingSharedObject;
import de.serdioa.common.pool.SharedObject;
import de.serdioa.common.pool.SynchronizedSharedObject;
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
 * A benchmark for comparing several implementations of a shared object. The benchmark runs tests with the following
 * parameters:
 * <p>
 * <strong>Type</strong>: the type of the shared object.
 * <ul>
 * <li>"pooled": the baseline test where the shared object is the pooled object itself. This baseline allows to estimate
 * an overhead of each shared object implementation.
 * <li>"sync": a hand-crafted shared object implemented using synchronization.
 * <li>"reflection-sync": a reflection-based shared object implemented using synchronization.
 * <li>"locking": a hand-crafted shared object implemented using read-write locks.
 * <li>"reflection-locking": a reflection-based shared object implemented using read-write locks.
 * </ul></p>
 * <p>
 * <strong>Tokens</strong>: the number of CPU tokens consumed by the test object implementation to simulate some work.
 * The test is executed with 0, 100 and 10000 tokens per invocation to estimate the overhead of each shared object
 * implementation with respect to method calls of different complexity. An overhead of a particular shared object
 * implementation could be very high when calling a trivial method, but the same overhead could be negligible when
 * calling a complicated and slow method.
 * <p>
 * <strong>Sharing mode</strong>: how objects are shared between parallel threads which run the test.
 * <ul>
 * <li>"shared": the pooled object and the shared object are shared between all threads.
 * <li>"thread": each thread has a thread-specific pooled object and shared object.
 * <li>"mixed" the pooled object is shared between all threads, but each thread has it's own pooled object.
 * </ul>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class SharedObjectBenchmark {

    // Abstract base class for the test state.
    public static abstract class AbstractState {

        @Param({"pooled", "locking", "sync", "reflection-locking", "reflection-sync"})
        public String type;

        @Param({"0", "100", "10000"})
        public int tokens;


        protected TestObject buildSharedObject(PooledTestObject pooledObject) {
            switch (this.type) {
                case "pooled":
                    return pooledObject;
                case "sync":
                    return new SynchronizedSharedTestObject(pooledObject);
                case "reflection-sync":
                    return SynchronizedSharedObject.factory(SharedTestObject.class).createShared(pooledObject, () -> {
                    });
                case "locking":
                    return new LockingSharedTestObject(pooledObject);
                case "reflection-locking":
                    return LockingSharedObject.factory(SharedTestObject.class).createShared(pooledObject, () -> {
                    });
                default:
                    throw new IllegalArgumentException("Unexpected type of the shared counter: " + this.type);
            }
        }


        protected void disposeSharedObject(TestObject sharedObject) {
            if (sharedObject instanceof SharedObject) {
                ((SharedObject) sharedObject).dispose();
            }
        }
    }


    // Test state where both pooled and shared object are shared by all threads.
    @State(Scope.Benchmark)
    public static class SharedState extends AbstractState {

        private PooledTestObject pooled;
        private TestObject shared;


        @Setup
        public void setup() {
            this.pooled = new PooledTestObject();
            this.shared = buildSharedObject(this.pooled);
        }


        @TearDown
        public void tearDown() {
            this.disposeSharedObject(this.shared);
            this.shared = null;
            this.pooled = null;
        }
    }


    // Test state where both pooled and shared object are specific to each thread.
    @State(Scope.Thread)
    public static class ThreadState extends AbstractState {

        private PooledTestObject pooled;
        private TestObject shared;


        @Setup
        public void setup() {
            this.pooled = new PooledTestObject();
            this.shared = buildSharedObject(this.pooled);
        }


        @TearDown
        public void tearDown() {
            this.disposeSharedObject(this.shared);
            this.shared = null;
            this.pooled = null;
        }
    }


    // Test state where pooled object is shared between threads, but shared object is specific to each thread.
    @State(Scope.Thread)
    public static class MixedState extends AbstractState {

        // The test pooled object shared by all threads.
        // During the initialization, access to the pooled object is guarded by the pooledLock.
        private static PooledTestObject pooled;
        private static final Object pooledLock = new Object();

        private TestObject shared;


        @Setup
        public void setup() {
            PooledTestObject pooledSnapshot;
            synchronized (MixedState.pooledLock) {
                // The first thread in the synchronization block initializes the pooled object.
                if (MixedState.pooled == null) {
                    MixedState.pooled = new PooledTestObject();
                }
                pooledSnapshot = MixedState.pooled;
            }

            this.shared = buildSharedObject(pooledSnapshot);
        }


        @TearDown
        public void tearDown() {
            this.disposeSharedObject(this.shared);
            this.shared = null;

            synchronized (MixedState.pooledLock) {
                MixedState.pooled = null;
            }
        }
    }


    @Benchmark
    public void testShared(SharedState state) {
        state.shared.run(state.tokens);
    }


    @Benchmark
    public void testThread(ThreadState state) {
        state.shared.run(state.tokens);
    }


    @Benchmark
    public void testMixed(MixedState state) {
        state.shared.run(state.tokens);
    }


    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SharedObjectBenchmark.class.getSimpleName())
                .forks(1)
                .syncIterations(true)
                .build();

        new Runner(opt).run();
    }
}
