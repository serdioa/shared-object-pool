package de.serdioa.common.pool;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import de.serdioa.common.pool.sample.PooledCounter;
import de.serdioa.common.pool.sample.PooledCounterFactory;
import de.serdioa.common.pool.sample.SharedCounter;


public class LockingSharedObjectPoolValidateTest {

    public static void main(String[] args) throws Exception {
        LockingSharedObjectPoolValidateTest test = new LockingSharedObjectPoolValidateTest();
        test.runRepeated(4, 4, 1, 2000, 100);
    }


    private LockingSharedObjectPool<String, SharedCounter, PooledCounter> buildImmediatePool() {
        PooledObjectFactory<String, PooledCounter> pof = new PooledCounterFactory();
        SharedObjectFactory<PooledCounter, SharedCounter> sof = SynchronizedSharedObject.factory(SharedCounter.class);

        return new LockingSharedObjectPool.Builder<String, SharedCounter, PooledCounter>()
                .setPooledObjectFactory(pof)
                .setSharedObjectFactory(sof)
                .build();
    }


    private LockingSharedObjectPool<String, SharedCounter, PooledCounter> buildDelayedPool(long disposeDelayMillis) {
        PooledObjectFactory<String, PooledCounter> pof = new PooledCounterFactory();
        SharedObjectFactory<PooledCounter, SharedCounter> sof = SynchronizedSharedObject.factory(SharedCounter.class);

        return new LockingSharedObjectPool.Builder<String, SharedCounter, PooledCounter>()
                .setPooledObjectFactory(pof)
                .setSharedObjectFactory(sof)
                .setDisposeThreads(1)
                .setIdleDisposeTimeMillis(disposeDelayMillis)
                .build();
    }


    private int runRepeated(int keys, int workers, int disposeDelayMillis, long durationMillis, int rounds) throws InterruptedException {
        int totalPooledObjectsCount = 0;
        for (int i = 0; i < rounds; ++i) {
            int pooledObjectsCount = this.run(keys, workers, disposeDelayMillis, durationMillis);
            totalPooledObjectsCount += pooledObjectsCount;

            System.out.println("Pooled objects count: " + pooledObjectsCount);
            System.out.println("Total pooled objects count: " + totalPooledObjectsCount);
        }

        return totalPooledObjectsCount;
    }


    private int run(int keys, int workers, int disposeDelayMillis, long durationMillis) throws InterruptedException {
        LockingSharedObjectPool<String, SharedCounter, PooledCounter> pool;
        if (disposeDelayMillis <= 0) {
            pool = buildImmediatePool();
        } else {
            pool = buildDelayedPool(disposeDelayMillis);
        }

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(workers);
        AtomicBoolean stopping = new AtomicBoolean(false);

        Thread[] threads = new Thread[workers];
        for (int i = 0; i < workers; ++i) {
            String sharedObjectKey = String.valueOf(i % keys);
            Worker worker = new Worker(pool, startLatch, endLatch, sharedObjectKey, stopping);
            threads[i] = new Thread(worker);
            threads[i].start();
        }

        // Trigger workers.
        System.out.println("Starting workers");
        startLatch.countDown();

        // Wait for the workers to run.
        System.out.println("Running workers");
        Thread.sleep(durationMillis);

        // Ask workers to stop.
        System.out.println("Stopping workers");
        stopping.set(Boolean.TRUE);

        // Wait for workers to stop.
        endLatch.await();
        System.out.println("All workers stopped");

        // Wait for pooled objects to be disposed of.
        if (disposeDelayMillis > 0) {
            Thread.sleep(disposeDelayMillis + 100);
        }

        int pooledObjectsCount = pool.getPooledObjectsCount();

        pool.dispose();

        return pooledObjectsCount;
    }


    private static class Worker implements Runnable {

        private final SharedObjectPool<String, SharedCounter> pool;
        private final CountDownLatch startLatch;
        private final CountDownLatch endLatch;
        private final String key;
        private final AtomicBoolean stopping;


        public Worker(SharedObjectPool<String, SharedCounter> pool,
                CountDownLatch startLatch,
                CountDownLatch endLatch,
                String key,
                AtomicBoolean stopping) {
            this.pool = Objects.requireNonNull(pool);
            this.startLatch = Objects.requireNonNull(startLatch);
            this.endLatch = Objects.requireNonNull(endLatch);
            this.key = Objects.requireNonNull(key);
            this.stopping = stopping;
        }


        @Override
        public void run() {
            try {
                this.startLatch.await();

                while (stopping.get() == Boolean.FALSE) {
                    SharedCounter counter = this.pool.get(key);
                    counter.increment();
                    counter.dispose();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                this.endLatch.countDown();
            }
        }
    }
}
