package de.serdioa.common.pool;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;


public class DisposeWeakReferencesTest {
    public static void main(String [] args) throws Exception {
        new DisposeWeakReferencesTest().run();
    }


    public static class MyObject {
        private final long id;

        public MyObject(long id) {
            this.id = id;
        }

        public long getId() {
            return this.id;
        }

        @Override
        public String toString() {
            return "MyObject[" + this.id + "]";
        }
    }


    private static class MyObjectWeakRef extends WeakReference<MyObject> {
        private final long id;
        private Thread releasedBy;
        private boolean releasedDirect;

        MyObjectWeakRef(MyObject obj, ReferenceQueue<? super MyObject> refQueue) {
            super(obj, refQueue);
            this.id = obj.getId();
        }

        public long getId() {
            return this.id;
        }


        public void setReleased(boolean direct) {
            assert (Thread.holdsLock(this));
            this.releasedBy = Thread.currentThread();
            this.releasedDirect = direct;
        }


        public Thread getReleasedBy() {
            assert (Thread.holdsLock(this));
            return this.releasedBy;
        }


        public String getReleasedByName() {
            assert (Thread.holdsLock(this));
            return (this.releasedBy == null ? null : this.releasedBy.getName());
        }


        public boolean isReleasedDirect() {
            assert (Thread.holdsLock(this));
            return this.releasedDirect;
        }


        @Override
        public String toString() {
            return "MyObjectWeakRef[" + this.id + ", releasedBy=" + this.releasedBy + "]";
        }
    }


    private final AtomicLong idGen = new AtomicLong();
    private final ConcurrentMap<Long, MyObjectWeakRef> refMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, MyObjectWeakRef> releasedRefMap = new ConcurrentHashMap<>();
    private final ReferenceQueue<MyObject> refQueue = new ReferenceQueue<>();


    private void reap() {
        System.out.println("Reaper has been started");
        try {
            while (true) {
                MyObjectWeakRef ref = (MyObjectWeakRef) this.refQueue.remove();
                reap(ref);
            }
        } catch (InterruptedException ex) {
            System.out.println("Reaper has been interrupted");
        }
    }


    private void reap(MyObjectWeakRef ref) {
        this.release(ref.getId(), false);
    }


    private MyObject acquire() {
        long id = this.idGen.getAndIncrement();
        MyObject obj = new MyObject(id);
        MyObjectWeakRef ref = new MyObjectWeakRef(obj, this.refQueue);

        MyObjectWeakRef prev = this.refMap.putIfAbsent(id, ref);
        if (prev != null) {
            System.out.println("acquire: weakRef[" + id + "] already exist");
        }

        return new MyObject(id);
    }


    private void release(MyObject obj) {
        this.release(obj.id, true);
    }


    private void release(long id, boolean direct) {
        MyObjectWeakRef ref = this.refMap.remove(id);
        if (ref == null) {
            MyObjectWeakRef releasedRef = this.releasedRefMap.get(id);
            if (releasedRef == null) {
                System.out.println("release (" + (direct ? "direct" : "ref") + "): weakRef[" + id + "] does not exist, releasedWeakRef[" + id + "] does not exist");
            } else {
                synchronized (releasedRef) {
                    String releasedByName = releasedRef.getReleasedByName();
                    boolean releasedDirect = releasedRef.isReleasedDirect();

                    if (direct || !releasedDirect) {
                        System.out.println("release (" + (direct ? "direct" : "ref") + "): weakRef[" + id + "] does not exist, releasedWeakRef[" + id + "] released by " + releasedByName);
                    } else {
                        // Standard case: attempt to release weak ref after standard release.
                    }
                }
            }
        } else {
            synchronized (ref) {
                String releasedByName = ref.getReleasedByName();
                if (releasedByName == null) {
                    // Expected case
                    // System.out.println("release (" + (direct ? "direct" : "ref") + "): weakRef[" + id + "] exist, releasedWeakRef[" + id + "] does not exist");
                    ref.setReleased(direct);
                    this.releasedRefMap.put(id, ref);
                } else {
                    System.out.println("release (" + (direct ? "direct" : "ref") + "): weakRef[" + id + "] exist, releasedWeakRef[" + id + "] released by " + releasedByName);
                }
            }
        }
    }


    private long workerRun(CountDownLatch startLatch, int workerId) {
        try {
            startLatch.await();
        } catch (InterruptedException ex) {
            System.out.println("Worker #" + workerId + " has been interrupted while waiting for start");
        }

        long sum = 0;
        long i = 0;
        while (true) {
            sum += workerIterate();
            if (sum == System.nanoTime()) {
                break;
            }

            if (i++ % 1000000 == 0) {
                System.out.println("worker " + workerId + ": " + i);
            }
        }
        System.out.println("Worker #" + workerId + " stopped, sum=" + sum);
        return sum;
    }


    private long workerIterate() {
        MyObject obj = acquire();
        long val = obj.getId();
        release(obj);

        return val;
    }


    public void run() throws Exception {
        final CountDownLatch startLatch = new CountDownLatch(1);

        final Thread reaper = new Thread(this::reap, "reaper");
        reaper.start();

        final int threadCount = 16;
        System.out.println("Creating " + threadCount + " worker threads");
        Thread [] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; ++i) {
            final int workerId = i;
            threads[i] = new Thread(() -> {
                this.workerRun(startLatch, workerId);
            }, "worker-" + i);
            threads[i].start();
        }

        System.out.println("Starting " + threadCount + " worker threads");
        startLatch.countDown();
        System.out.println(threadCount + " worker threads have been started");
    }
}
