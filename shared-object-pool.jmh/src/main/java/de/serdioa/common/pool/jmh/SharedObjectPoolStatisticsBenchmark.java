package de.serdioa.common.pool.jmh;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import de.serdioa.common.pool.AbstractSharedObjectPool;
import de.serdioa.common.pool.ConcurrentSharedObjectPool;
import de.serdioa.common.pool.DefaultPooledObjectFactory;
import de.serdioa.common.pool.LockingSharedObject;
import de.serdioa.common.pool.LockingSharedObjectPool;
import de.serdioa.common.pool.NoOpStackTraceProvider;
import de.serdioa.common.pool.PooledObjectFactory;
import de.serdioa.common.pool.SharedObjectFactory;
import de.serdioa.common.pool.SharedObjectPool;
import de.serdioa.common.pool.SharedObjectPoolStatsListener;
import de.serdioa.common.pool.SynchronizedSharedObjectPool;
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
 * Benchmark for SharedObjectPool.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class SharedObjectPoolStatisticsBenchmark {

    @State(Scope.Benchmark)
    public static class BenchmarkState {

        /**
         * The type of the object pool implementation.
         */
        @Param({"sync"})
        public String type;

        /**
         * The type of the metrics listener.
         */
        @Param({"none", "simple"})
        public String statListenerType;

        /**
         * The number of objects in the pool.
         */
        @Param({"1000"})
        public int pooledObjectsCount;

        /**
         * The percentage of objects for which a shared object is permanently held, to keep the pooled object from
         * disposal.
         */
        @Param({"0.5"})
        public double sharedObjectKeepPercentage;

        /**
         * The object pool.
         */
        public SharedObjectPool<Integer, SharedTestObject> pool;

        /**
         * Shared objects which we hold permanently to keep them from disposal.
         */
        public SharedTestObject[] keepSharedObjects;

        /**
         * The simple performance statistics listener.
         */
        private SimpleSharedObjectPoolStatsListener statListener;


        @Setup
        public void setup() {
            System.out.println("SharedObjectPoolStatisticsBenchmark.setup()");

            PooledObjectFactory<Integer, TestObject> pooledObjectFactory =
                    new DefaultPooledObjectFactory.Builder<Integer, TestObject>()
                            .setCreator(key -> new PooledTestObject())
                            .build();
            SharedObjectFactory<TestObject, SharedTestObject> sharedObjectFactory =
                    LockingSharedObject.factory(SharedTestObject.class);

            switch (type) {
                case "sync":
                    this.pool = new SynchronizedSharedObjectPool.Builder<Integer, SharedTestObject, TestObject>()
                            .setPooledObjectFactory(pooledObjectFactory)
                            .setSharedObjectFactory(sharedObjectFactory)
                            .setStackTraceProvider(new NoOpStackTraceProvider())
                            .build();
                    break;

                case "locking":
                    this.pool = new LockingSharedObjectPool.Builder<Integer, SharedTestObject, TestObject>()
                            .setPooledObjectFactory(pooledObjectFactory)
                            .setSharedObjectFactory(sharedObjectFactory)
                            .setStackTraceProvider(new NoOpStackTraceProvider())
                            .build();
                    break;

                case "concurrent":
                    this.pool = new ConcurrentSharedObjectPool.Builder<Integer, SharedTestObject, TestObject>()
                            .setPooledObjectFactory(pooledObjectFactory)
                            .setSharedObjectFactory(sharedObjectFactory)
                            .setStackTraceProvider(new NoOpStackTraceProvider())
                            .build();
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected type of the object pool: " + this.type);
            }

            // Prepare keys of pooled objects to be kept.
            int sharedObjectKeepCount = (int) (this.pooledObjectsCount * this.sharedObjectKeepPercentage);
            if (sharedObjectKeepCount > 0) {
                // Get keys 0, ..., pooledObjectsCount-1, shuffle in random order and take the required percentage.
                List<Integer> keys = new ArrayList<>();
                for (int i = 0; i < this.pooledObjectsCount; ++i) {
                    keys.add(i);
                }
                Collections.shuffle(keys);
                keys = keys.subList(0, sharedObjectKeepCount);

                this.keepSharedObjects = new SharedTestObject[sharedObjectKeepCount];
                for (int i = 0; i < sharedObjectKeepCount; ++i) {
                    Integer key = keys.get(i);
                    this.keepSharedObjects[i] = this.pool.get(key);
                }
            } else {
                this.keepSharedObjects = new SharedTestObject[0];
            }

            // Create a register an appropriate performance statistics listener.
            if ("simple".equals(this.statListenerType)) {
                this.statListener = new SimpleSharedObjectPoolStatsListener();
                ((AbstractSharedObjectPool<?, ?, ?>) this.pool).addSharedObjectPoolStatsListener(this.statListener);
            }
        }


        @TearDown
        public void tearDown() {
            System.out.println("SharedObjectPoolStatisticsBenchmark.tearDown()");

            // Print the pool's statistics.
            System.out.println("pooled=" + ((AbstractSharedObjectPool<?, ?, ?>) this.pool).getPooledObjectsCount());
            System.out.println("unusedPool=" + ((AbstractSharedObjectPool<?, ?, ?>) this.pool)
                    .getUnusedPooledObjectsCount());
            System.out.println("shared=" + ((AbstractSharedObjectPool<?, ?, ?>) this.pool).getSharedObjectsCount());

            if ("simple".equals(this.statListenerType)) {
                System.out.println("hitCount=" + this.statListener.getHitCount());
                System.out.println("avgHitDuration=" + this.statListener.getAvgHitDuration());
                System.out.println("missCount=" + this.statListener.getMissCount());
                System.out.println("avgMissDuration=" + this.statListener.getAvgMissDuration());

                System.out.println("createdSuccessCount=" + this.statListener.getCreatedSuccessCount());
                System.out.println("avgCreatedSuccessDuration=" + this.statListener.getAvgCreatedSuccessDuration());
                System.out.println("createdExceptionCount=" + this.statListener.getCreatedExceptionCount());
                System.out.println("avgCreatedExceptionDuration=" + this.statListener.getAvgCreatedExceptionDuration());

                System.out.println("initializedSuccessCount=" + this.statListener.getInitializedSuccessCount());
                System.out.println("avgInitializedSuccessDuration=" + this.statListener
                        .getAvgInitializedSuccessDuration());
                System.out.println("initializedExceptionCount=" + this.statListener.getInitializedExceptionCount());
                System.out.println("avgInitializedExceptionDuration=" + this.statListener
                        .getAvgInitializedExceptionDuration());

                System.out.println("disposedSuccessCount=" + this.statListener.getDisposedSuccessCount());
                System.out.println("avgDisposedSuccessDuration=" + this.statListener.getAvgDisposedSuccessDuration());
                System.out.println("disposedExceptionCount=" + this.statListener.getDisposedExceptionCount());
                System.out
                        .println("avgDisposedExceptionDuration=" + this.statListener.getAvgDisposedExceptionDuration());
            }

            // Dispose of shared objects we are keeping.
            for (int i = 0; i < this.keepSharedObjects.length; ++i) {
                this.keepSharedObjects[i].dispose();
            }

            // Remove the statistics listener.
            if ("simple".equals(this.statListenerType)) {
                ((AbstractSharedObjectPool<?, ?, ?>) this.pool).removeSharedObjectPoolStatsListener(this.statListener);
            }

            // Dispose of the pool.
            ((AbstractSharedObjectPool<?, ?, ?>) this.pool).dispose();
        }
    }


    private static class SimpleSharedObjectPoolStatsListener implements SharedObjectPoolStatsListener {

        private final AtomicLong hitCount = new AtomicLong();
        private final AtomicLong hitDuration = new AtomicLong();
        private final AtomicLong missCount = new AtomicLong();
        private final AtomicLong missDuration = new AtomicLong();
        private final AtomicLong createdSuccessCount = new AtomicLong();
        private final AtomicLong createdSuccessDuration = new AtomicLong();
        private final AtomicLong createdExceptionCount = new AtomicLong();
        private final AtomicLong createdExceptionDuration = new AtomicLong();
        private final AtomicLong initializedSuccessCount = new AtomicLong();
        private final AtomicLong initializedSuccessDuration = new AtomicLong();
        private final AtomicLong initializedExceptionCount = new AtomicLong();
        private final AtomicLong initializedExceptionDuration = new AtomicLong();
        private final AtomicLong disposedSuccessCount = new AtomicLong();
        private final AtomicLong disposedSuccessDuration = new AtomicLong();
        private final AtomicLong disposedExceptionCount = new AtomicLong();
        private final AtomicLong disposedExceptionDuration = new AtomicLong();


        @Override
        public void onSharedObjectGet(long durationNanos, boolean hit) {
            if (hit) {
                this.hitCount.incrementAndGet();
                this.hitDuration.addAndGet(durationNanos);
            } else {
                this.missCount.incrementAndGet();
                this.missDuration.addAndGet(durationNanos);
            }
        }


        @Override
        public void onPooledObjectCreated(long durationNanos, boolean success) {
            if (success) {
                this.createdSuccessCount.incrementAndGet();
                this.createdSuccessDuration.addAndGet(durationNanos);
            } else {
                this.createdExceptionCount.incrementAndGet();
                this.createdExceptionDuration.addAndGet(durationNanos);
            }
        }


        @Override
        public void onPooledObjectInitialized(long durationNanos, boolean success) {
            if (success) {
                this.initializedSuccessCount.incrementAndGet();
                this.initializedSuccessDuration.addAndGet(durationNanos);
            } else {
                this.initializedExceptionCount.incrementAndGet();
                this.initializedExceptionDuration.addAndGet(durationNanos);
            }
        }


        @Override
        public void onPooledObjectDisposed(long durationNanos, boolean success) {
            if (success) {
                this.disposedSuccessCount.incrementAndGet();
                this.disposedSuccessDuration.addAndGet(durationNanos);
            } else {
                this.disposedExceptionCount.incrementAndGet();
                this.disposedExceptionDuration.addAndGet(durationNanos);
            }
        }


        public long getHitCount() {
            return this.hitCount.get();
        }


        public long getAvgHitDuration() {
            long count = this.hitCount.get();
            return (count != 0 ? this.hitDuration.get() / count : 0);
        }


        public long getMissCount() {
            return this.missCount.get();
        }


        public long getAvgMissDuration() {
            long count = this.missCount.get();
            return (count != 0 ? this.missDuration.get() / count : 0);
        }


        public long getCreatedSuccessCount() {
            return this.createdSuccessCount.get();
        }


        public long getAvgCreatedSuccessDuration() {
            long count = this.createdSuccessCount.get();
            return (count != 0 ? this.createdSuccessDuration.get() / count : 0);
        }


        public long getCreatedExceptionCount() {
            return this.createdExceptionCount.get();
        }


        public long getAvgCreatedExceptionDuration() {
            long count = this.createdExceptionCount.get();
            return (count != 0 ? this.createdExceptionDuration.get() / count : 0);
        }


        public long getInitializedSuccessCount() {
            return this.initializedSuccessCount.get();
        }


        public long getAvgInitializedSuccessDuration() {
            long count = this.initializedSuccessCount.get();
            return (count != 0 ? this.initializedSuccessDuration.get() / count : 0);
        }


        public long getInitializedExceptionCount() {
            return this.initializedExceptionCount.get();
        }


        public long getAvgInitializedExceptionDuration() {
            long count = this.initializedExceptionCount.get();
            return (count != 0 ? this.initializedExceptionDuration.get() / count : 0);
        }


        public long getDisposedSuccessCount() {
            return this.disposedSuccessCount.get();
        }


        public long getAvgDisposedSuccessDuration() {
            long count = this.disposedSuccessCount.get();
            return (count != 0 ? this.disposedSuccessDuration.get() / count : 0);
        }


        public long getDisposedExceptionCount() {
            return this.disposedExceptionCount.get();
        }


        public long getAvgDisposedExceptionDuration() {
            long count = this.disposedExceptionCount.get();
            return (count != 0 ? this.disposedExceptionDuration.get() / count : 0);
        }
    }


    @Benchmark
    public SharedTestObject testGet(BenchmarkState state) {
        Integer key = ThreadLocalRandom.current().nextInt(state.pooledObjectsCount);
        SharedTestObject shared = state.pool.get(key);
        shared.dispose();

        return shared;
    }


    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SharedObjectPoolStatisticsBenchmark.class.getSimpleName())
                .forks(1)
                .syncIterations(true)
                .build();

        new Runner(opt).run();
    }
}
