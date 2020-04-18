package de.serdioa.common.pool.test;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import de.serdioa.common.pool.ConcurrentSharedObjectPool;
import de.serdioa.common.pool.InvalidKeyException;
import de.serdioa.common.pool.LockingSharedObject;
import de.serdioa.common.pool.LockingSharedObjectPool;
import de.serdioa.common.pool.PooledObjectFactory;
import de.serdioa.common.pool.SharedObjectFactory;
import de.serdioa.common.pool.SharedObjectPool;
import de.serdioa.common.pool.SynchronizedSharedObject;
import de.serdioa.common.pool.SynchronizedSharedObjectPool;
import de.serdioa.common.pool.sample.LockingSharedCounter;
import de.serdioa.common.pool.sample.PooledCounter;
import de.serdioa.common.pool.sample.SharedCounter;
import de.serdioa.common.pool.sample.SynchronizedSharedCounter;


public class PoolTest {

    // Configuration for this test.
    private final Configuration config;

    // Worker threads.
    private Thread[] threads;

    // Latch for starting threads.
    private CountDownLatch startLatch;

    // Latch for stopping threads.
    private CountDownLatch stopLatch;

    // The object pool being tested.
    private SharedObjectPool<Integer, SharedCounter> pool;

    // Tracks generation of each counter. Each time when a new instance of a pooled counter is created, the generation
    // is increased.
    private AtomicInteger[] generationCounter;


    public PoolTest(Configuration config) {
        this.config = Objects.requireNonNull(config);
    }


    public void run() {
        this.build();
        this.start();
        this.waitAndStop();
    }


    private void build() {
        this.buildGenerationCounters();
        this.buildPool();
        this.buildWorkers();
    }


    private void buildGenerationCounters() {
        this.generationCounter = new AtomicInteger[this.config.getObjects()];
        for (int i = 0; i < this.generationCounter.length; ++i) {
            this.generationCounter[i] = new AtomicInteger();
        }
    }


    // Build the pool of shared objects based on the configuration.
    private void buildPool() {
        PooledObjectFactory<Integer, PooledCounter> pooledObjectFactory = new TestPooledObjectFactory();
        SharedObjectFactory<PooledCounter, SharedCounter> sharedObjectFactory = buildSharedObjectFactory();

        final PoolType poolType = this.config.getPoolType();
        if (poolType == PoolType.SYNCHRONIZED) {
            this.pool = new SynchronizedSharedObjectPool.Builder<Integer, SharedCounter, PooledCounter>()
                    .setPooledObjectFactory(pooledObjectFactory)
                    .setSharedObjectFactory(sharedObjectFactory)
                    .build();
        } else if (poolType == PoolType.LOCKING) {
            this.pool = new LockingSharedObjectPool.Builder<Integer, SharedCounter, PooledCounter>()
                    .setPooledObjectFactory(pooledObjectFactory)
                    .setSharedObjectFactory(sharedObjectFactory)
                    .build();
        } else if (poolType == PoolType.CONCURRENT) {
            this.pool = new ConcurrentSharedObjectPool.Builder<Integer, SharedCounter, PooledCounter>()
                    .setPooledObjectFactory(pooledObjectFactory)
                    .setSharedObjectFactory(sharedObjectFactory)
                    .build();
        } else {
            throw new IllegalArgumentException("Unexpected pool type: " + poolType);
        }
    }


    private SharedObjectFactory<PooledCounter, SharedCounter> buildSharedObjectFactory() {
        final ObjectType objectType = PoolTest.this.config.getObjectType();

        switch (objectType) {
            case LOCKING:
                return LockingSharedCounter::new;
            case SYNCHRONIZED:
                return SynchronizedSharedCounter::new;
            case REFLECTION_LOCKING:
                return LockingSharedObject.factory(SharedCounter.class);
            case REFLECTION_SYNCHRONIZED:
                return SynchronizedSharedObject.factory(SharedCounter.class);
            default:
                throw new IllegalArgumentException("Unexpected object type: " + objectType);
        }
    }


    private void buildWorkers() {
        final int threadCount = this.config.getThreads();

        this.startLatch = new CountDownLatch(1);
        this.stopLatch = new CountDownLatch(this.config.getThreads());

        this.threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; ++i) {
            Runnable worker = new Worker(i, this.startLatch, this.stopLatch);
            this.threads[i] = new Thread(worker);
        }
    }


    private void start() {
        // Start worker threads.
        System.out.println("Starting " + this.config.getThreads() + " workers");
        for (Thread thread : this.threads) {
            thread.start();
        }

        // Trigger all workers.
        this.startLatch.countDown();
        System.out.println("Started " + this.config.getThreads() + " workers");
    }


    private void waitAndStop() {
        System.out.println("Waiting " + this.config.getRunMillis() + " ms");
        try {
            int count = 0;
            long now = System.currentTimeMillis();
            final long sleepUntil = now + this.config.getRunMillis();

            while (sleepUntil > now) {
                long sleepMillis = 1000;
                if (now + sleepMillis > sleepUntil) {
                    sleepMillis = sleepUntil - now;
                }
                if (sleepMillis > 0) {
                    Thread.sleep(sleepMillis);
                }

                count++;
                if (count % 100 == 0) {
                    System.out.println(" " + count);
                } else if (count % 10 == 0) {
                    System.out.print('+');
                    System.out.flush();
                } else {
                    System.out.print('.');
                    System.out.flush();
                }

                now = System.currentTimeMillis();
            }
            System.out.println();
        } catch (InterruptedException ex) {
            // Continue to shutdown workers.
        }

        System.out.println("Shutting down " + this.config.getThreads() + " workers");
        for (Thread thread : this.threads) {
            thread.interrupt();
        }

        System.out.println("Waiting for " + this.config.getThreads() + " workers to stop");
        try {
            this.stopLatch.await();
            System.out.println("All workers stopped");
        } catch (InterruptedException ex) {
            System.out.println("Interrupted when waiting for workers to stop");
        }
    }


    private class TestPooledObjectFactory implements PooledObjectFactory<Integer, PooledCounter> {

        @Override
        public PooledCounter create(Integer key) throws InvalidKeyException {
            return new PooledCounter(String.valueOf(key));
        }


        @Override
        public void initialize(PooledCounter counter) {
            counter.initialize();

            int counterId = Integer.parseInt(counter.getKey());
            PoolTest.this.generationCounter[counterId].incrementAndGet();
        }


        @Override
        public void dispose(PooledCounter counter) {
            counter.dispose();
        }
    }


    private class Worker implements Runnable {

        private final int id;
        private final CountDownLatch startLatch;
        private final CountDownLatch stopLatch;

        private final SharedObjectPool<Integer, SharedCounter> pool = PoolTest.this.pool;

        private final int objectsCount = PoolTest.this.config.getObjects();

        private final int[] totalValues = new int[this.objectsCount];
        private final int[] observedValues = new int[this.objectsCount];
        private final int[] observedGenerations = new int[this.objectsCount];


        public Worker(int id, CountDownLatch startLatch, CountDownLatch stopLatch) {
            this.id = id;
            this.startLatch = Objects.requireNonNull(startLatch);
            this.stopLatch = Objects.requireNonNull(stopLatch);
        }


        @Override
        public void run() {
            try {
                // Wait for a command to start.
                this.startLatch.await();
            } catch (InterruptedException ex) {
                // Interrupted before started.
                Thread.currentThread().interrupt();
                return;
            }

            System.out.println("Worker " + this.id + " started");

            try {
                while (!Thread.currentThread().isInterrupted()) {
                    count();
                }
            } finally {
                // Indicate that this worker is finished.
                this.stopLatch.countDown();

                StringBuilder sb = new StringBuilder("Worker ").append(this.id).append(" stopped\n");
                sb.append("    observedGenerations=").append(Arrays.toString(this.observedGenerations)).append("\n");
                sb.append("    observedValues=").append(Arrays.toString(this.observedValues)).append("\n");
                sb.append("    totalValues=").append(Arrays.toString(this.totalValues)).append("\n");

                System.out.println(sb);
            }
        }


        private void count() {
            // Decide which object index to use.
            int index = ThreadLocalRandom.current().nextInt(this.objectsCount);
            SharedCounter counter = this.pool.get(index);
            int value = counter.increment();

            // Since we are keeping the shared counter, it can't be destroyed, and the generation shall remain constant.
            int generation = PoolTest.this.generationCounter[index].get();

            // Dispose of the counter.
            counter.dispose();

            // Compare with previously observed values.
            int prevValue = this.observedValues[index];
            int prevGeneration = this.observedGenerations[index];

            if (generation == prevGeneration) {
                // The same generation as before: the counter must increase.
                if (value <= prevValue) {
                    System.out.println("ERROR: worker " + this.id + ", index=" + index + ", generation unchanged ("
                            + generation + "), prevValue=" + prevValue + ", value=" + value);
                }
            } else if (generation > prevGeneration) {
                // The generation increased. Everything is possible with the counter. It started to count from 0 again,
                // so it may be less than before, but may be greater as well if it was counted enough times.
            } else {
                // The generation must only increase, never decrease.
                System.out.println("ERROR: worker " + this.id + ", index=" + index + ", prevGeneraion="
                        + prevGeneration + ", generation=" + generation);
            }

            // Update observed values.
            this.totalValues[index]++;
            this.observedValues[index] = value;
            this.observedGenerations[index] = generation;
        }
    }


    public static void main(String[] args) {
        Configuration config = Configuration.fromProperties(System.getProperties());
        PoolTest poolTest = new PoolTest(config);
        poolTest.run();
    }
}
