package de.serdioa.common.pool;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import de.serdioa.common.pool.sample.PooledCounter;
import de.serdioa.common.pool.sample.PooledCounterFactory;
import de.serdioa.common.pool.sample.LockingSharedCounter;


// Test for SynchronizedSharedObjectPool.
public class ConcurrencyTest_01 {
    public static void main(String [] args) throws Exception {
        new ConcurrencyTest_01().run();
    }


    private static final int THREADS_COUNT = 16;
    private static final int ITERATIONS = 1000000;
    private static final int BUCKETS = 1000;
    private final SynchronizedSharedObjectPool<String, LockingSharedCounter, PooledCounter> pool;

    public ConcurrencyTest_01() {
        this.pool = new SynchronizedSharedObjectPool<>();
        this.pool.setPooledObjectFactory(new PooledCounterFactory());
        this.pool.setSharedObjectFactory(LockingSharedCounter::new);
    }


    public void run() throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(THREADS_COUNT);

        TestRunner [] runners = new TestRunner[THREADS_COUNT];
        Thread [] threads = new Thread[THREADS_COUNT];
        for (int i = 0; i < THREADS_COUNT; ++i) {
            String key;
            switch (i % 4) {
                case 0: key = "AAA"; break;
                case 1: key = "BBB"; break;
                case 2: key = "CCC"; break;
                case 3: key = "DDD"; break;
                default: key = "unexpected";
            }

            String name = key + "_" + i;

            runners[i] = new TestRunner(name, this.pool, key, startLatch, endLatch);
            threads[i] = new Thread(runners[i], name);
            threads[i].start();
        }

        System.out.println("Starting runners");
        long startTimestamps = System.currentTimeMillis();
        startLatch.countDown();
        System.out.println("Waiting for runners to finish");
        endLatch.await();
        long endTimestamp = System.currentTimeMillis();
        System.out.println("Runners finished, runtime " + (endTimestamp - startTimestamps) + " ms");

        for (int i = 0; i < THREADS_COUNT; ++i) {
            runners[i].printStatistics();
        }
    }


    private static class TestRunner implements Runnable {
        private final String name;
        private final SynchronizedSharedObjectPool<String, LockingSharedCounter, PooledCounter> pool;
        private final String key;
        private final CountDownLatch startLatch;
        private final CountDownLatch endLatch;

        private final int [] statistics = new int[BUCKETS];
        private int maxCount = -1;

        TestRunner(String name, SynchronizedSharedObjectPool<String, LockingSharedCounter, PooledCounter> pool, String key,
                CountDownLatch startLatch, CountDownLatch endLatch) {
            this.name = name;
            this.pool = pool;
            this.key = key;
            this.startLatch = startLatch;
            this.endLatch = endLatch;
        }


        public String getName() {
            return this.name;
        }


        public int [] getStatistics() {
            return this.statistics;
        }


        @Override
        public void run() {
            try {
                this.startLatch.await();

                for (int i = 0; i < ITERATIONS; ++i) {
                    runIteration();
                }

                this.endLatch.countDown();

            } catch (InterruptedException ex) {
                System.err.println("TestRunner has been interrupted");
                Thread.currentThread().interrupt();
            }
        }


        private void runIteration() {
            LockingSharedCounter counter = this.pool.get(this.key);
            int count = counter.increment();
            counter.dispose();

            this.statistics[Math.min(count, BUCKETS - 1)]++;
            this.maxCount = Math.max(this.maxCount, count);
        }


        public void printStatistics() {
            System.out.println(this.name + ": (" + this.maxCount + ") " + Arrays.toString(this.statistics));
        }
    }
}
