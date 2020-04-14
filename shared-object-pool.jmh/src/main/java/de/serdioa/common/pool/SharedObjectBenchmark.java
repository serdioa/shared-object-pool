package de.serdioa.common.pool;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;


/**
 * A benchmark for various implementations of a shared object.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class SharedObjectBenchmark {

    // A test pooled object. The method run() just consumes CPU.
    public interface TestObject {

        void run(int tokens);
    }


    // A trivial implementation of the TestObject which uses JMH Blackhole to consume CPU.
    public static class PooledTestObject implements TestObject {

        @Override
        public void run(int tokens) {
            Blackhole.consumeCPU(tokens);
        }
    }


    public interface SharedTestObject extends TestObject, SharedObject {
    }


    // A hand-crafted implementation of shared object usign synchronization.
    public static class SynchronizedSharedTestObject implements SharedTestObject {

        private final TestObject pooled;

        private volatile boolean dummy = false;

        // @GuardedBy(mutex)
        private boolean disposed = false;

        private final Object mutex = new Object();


        public SynchronizedSharedTestObject(TestObject pooled) {
            this.pooled = Objects.requireNonNull(pooled);
        }


        @Override
        public void run(int tokens) {
            synchronized (this.mutex) {
                if (this.disposed) {
                    throw new IllegalStateException("Shared object is already disposed of");
                } else {
                    this.pooled.run(tokens);
                }
            }
        }


        @Override
        public void dispose() {
            synchronized (this.mutex) {
                if (this.disposed) {
                    throw new IllegalStateException("Shared object is already disposed of");
                } else {
                    this.disposed = true;

                    // This code actually never executes, but since dummy is volatile, JVM can't optimize it away
                    // and can't GC this object before the disposeCallback() above is finished.
                    if (dummy) {
                        this.run(0);
                    }
                }
            }
        }


        @Override
        public boolean isDisposed() {
            synchronized (this.mutex) {
                return this.disposed;
            }
        }
    }


    // A hand-crafted implementation of shared object usign locks.
    public static class LockingSharedTestObject implements SharedTestObject {

        private final TestObject pooled;

        private volatile boolean dummy = false;

        // @GuardedBy(lock)
        private boolean disposed = false;

        private final ReadWriteLock lock = new ReentrantReadWriteLock();


        public LockingSharedTestObject(TestObject pooled) {
            this.pooled = Objects.requireNonNull(pooled);
        }


        @Override
        public void run(int tokens) {
            final Lock sharedLock = this.lock.readLock();
            sharedLock.lock();
            try {
                if (this.disposed) {
                    throw new IllegalStateException("Shared object is already disposed of");
                } else {
                    this.pooled.run(tokens);
                }
            } finally {
                sharedLock.unlock();
            }
        }


        @Override
        public void dispose() {
            final Lock exclusiveLock = this.lock.writeLock();
            exclusiveLock.lock();
            try {
                if (this.disposed) {
                    throw new IllegalStateException("Shared object is already disposed of");
                } else {
                    this.disposed = true;

                    // This code actually never executes, but since dummy is volatile, JVM can't optimize it away
                    // and can't GC this object before the disposeCallback() above is finished.
                    if (dummy) {
                        this.run(0);
                    }
                }
            } finally {
                exclusiveLock.unlock();
            }
        }


        @Override
        public boolean isDisposed() {
            final Lock sharedLock = this.lock.readLock();
            sharedLock.lock();
            try {
                return this.disposed;
            } finally {
                sharedLock.unlock();
            }
        }
    }


    public static abstract class AbstractState {

        @Param({"pooled", "locking", "sync", "reflection-locking", "reflection-sync"})
        public String type;

        @Param({"0", "100", "10000"})
        public int tokens;

        public PooledTestObject pooled;
        public TestObject shared;


        @Setup
        public void setup() {
            this.pooled = new PooledTestObject();

            switch (this.type) {
                case "pooled":
                    this.shared = this.pooled;
                    break;
                case "locking":
                    this.shared = new LockingSharedTestObject(this.pooled);
                    break;
                case "sync":
                    this.shared = new SynchronizedSharedTestObject(this.pooled);
                    break;
                case "reflection-locking":
                    this.shared = LockingSharedObject.factory(SharedTestObject.class).createShared(
                            this.pooled, () -> {
                            });
                    break;
                case "reflection-sync":
                    this.shared = SynchronizedSharedObject.factory(SharedTestObject.class).createShared(
                            this.pooled, () -> {
                            });
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected type of the shared counter: " + this.type);
            }
        }


        @TearDown
        public void tearDown() {
            if (this.shared instanceof SharedObject) {
                ((SharedObject) this.shared).dispose();
            }

            this.shared = null;
            this.pooled = null;
        }
    }


    @State(Scope.Benchmark)
    public static class SharedState extends AbstractState {
    }


    @State(Scope.Thread)
    public static class ThreadState extends AbstractState {
    }


    @Benchmark
    public void testThread(ThreadState state) {
        state.shared.run(state.tokens);
    }


    @Benchmark
    public void testShared(SharedState state) {
        state.shared.run(state.tokens);
    }


    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SharedObjectBenchmark.class.getSimpleName())
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
