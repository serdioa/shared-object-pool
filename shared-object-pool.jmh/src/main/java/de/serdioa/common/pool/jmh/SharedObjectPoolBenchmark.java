package de.serdioa.common.pool.jmh;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import de.serdioa.common.pool.AbstractSharedObjectPool;
import de.serdioa.common.pool.ConcurrentSharedObjectPool;
import de.serdioa.common.pool.DefaultPooledObjectFactory;
import de.serdioa.common.pool.LockingSharedObject;
import de.serdioa.common.pool.LockingSharedObjectPool;
import de.serdioa.common.pool.NoOpStackTraceProvider;
import de.serdioa.common.pool.PooledObjectFactory;
import de.serdioa.common.pool.SharedObjectFactory;
import de.serdioa.common.pool.SharedObjectPool;
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
public class SharedObjectPoolBenchmark {

    @State(Scope.Benchmark)
    public static class BenchmarkState {

        /**
         * The type of the object pool implementation.
         */
        @Param({"sync", "locking", "concurrent"})
        public String type;

        /**
         * The number of objects in the pool.
         */
        @Param({"1", "10", "100", "1000"})
        public int pooledObjectsCount;

        /**
         * The percentage of objects for which a shared object is permanently held, to keep the pooled object from
         * disposal.
         */
        @Param({"0.0", "0.5", "1.0"})
        public double sharedObjectKeepPercentage;

        /**
         * The object pool.
         */
        public SharedObjectPool<Integer, SharedTestObject> pool;

        /**
         * Shared objects which we hold permanently to keep them from disposal.
         */
        public SharedTestObject[] keepSharedObjects;


        @Setup
        public void setup() {
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
        }


        @TearDown
        public void tearDown() {
            // Dispose of shared objects we are keeping.
            for (int i = 0; i < this.keepSharedObjects.length; ++i) {
                this.keepSharedObjects[i].dispose();
            }

            // Dispose of the pool.
            ((AbstractSharedObjectPool<?, ?, ?>) this.pool).dispose();
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
                .include(SharedObjectPoolBenchmark.class.getSimpleName())
                .forks(1)
                .syncIterations(true)
                .build();

        new Runner(opt).run();
    }
}
