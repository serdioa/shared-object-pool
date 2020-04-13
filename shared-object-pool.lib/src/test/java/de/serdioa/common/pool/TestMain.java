package de.serdioa.common.pool;

import de.serdioa.common.pool.sample.PooledCounterFactory;
import de.serdioa.common.pool.sample.LockingSharedCounter;
import de.serdioa.common.pool.sample.PooledCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TestMain {
    private static final Logger logger = LoggerFactory.getLogger(TestMain.class);

    public static void main(String [] args) throws Exception {
        logger.info("test");
        new TestMain().run();
    }

    public void run_1() throws Exception {
        PooledCounterFactory pdcf = new PooledCounterFactory();
        PooledCounter pdc = pdcf.create("AAA");
        pdc.initialize();

        LockingSharedCounter sdc = new LockingSharedCounter(pdc, () -> {
           System.out.println("Dispose callback called from SharedCounter[AAA]");
        });

        System.out.println("get=" + sdc.get());
        System.out.println("increment=" + sdc.increment());
        System.out.println("increment=" + sdc.increment());
        System.out.println("decrement=" + sdc.decrement());
        System.out.println("decrement=" + sdc.decrement());

        sdc.dispose();
        pdc.dispose();

        try {
            sdc.get();
        } catch (Exception ex) {
            System.out.println("Attempt to call get() on disposed SharedCounter throws exception: " + ex.getMessage());
        }

        try {
            pdc.get();
        } catch (Exception ex) {
            System.out.println("Attempt to call get() on disposed PooledCounter throws exception: " + ex.getMessage());
        }
    }

    public void run() throws Exception {
        PooledCounterFactory pdcf = new PooledCounterFactory();

        SynchronizedSharedObjectPool<String, LockingSharedCounter, PooledCounter> pool
                = new SynchronizedSharedObjectPool<>();
        pool.setPooledObjectFactory(pdcf);
        pool.setSharedObjectFactory(LockingSharedCounter::new);

        System.out.println("Getting AAA[1]");
        LockingSharedCounter aaa_1 = pool.get("AAA");
        System.out.println("AAA[1].get=" + aaa_1.get());
        System.out.println("AAA[1].increment=" + aaa_1.increment());

        System.out.println("Getting AAA[2]");
        LockingSharedCounter aaa_2 = pool.get("AAA");
        System.out.println("AAA[2].get=" + aaa_2.get());
        System.out.println("AAA[2].increment=" + aaa_2.increment());

        System.out.println("AAA[1].get=" + aaa_1.get());
        System.out.println("AAA[1].increment=" + aaa_1.increment());

        System.out.println("AAA[2].get=" + aaa_2.get());
        System.out.println("AAA[2].increment=" + aaa_2.increment());

        System.out.println("Getting BBB[1]");
        LockingSharedCounter bbb_1 = pool.get("BBB");
        System.out.println("BBB[1].get=" + bbb_1.get());
        System.out.println("BBB[1].increment=" + bbb_1.increment());

        System.out.println("Disposing of AAA[1]");
        aaa_1.dispose();
        try {
            System.out.println("!!! AAA[1].get=" + aaa_1.get());
        } catch (Exception ex) {
            System.out.println("Attempt to call get() on disposed AAA[1] throws exception: " + ex.getMessage());
        }

        System.out.println("AAA[2].get=" + aaa_2.get());
        System.out.println("AAA[2].increment=" + aaa_2.increment());

        System.out.println("BBB[1].get=" + bbb_1.get());
        System.out.println("BBB[1].increment=" + bbb_1.increment());

        System.out.println("Disposing of BBB[1]");
        bbb_1.dispose();
        try {
            System.out.println("!!! BBB[1].get=" + bbb_1.get());
        } catch (Exception ex) {
            System.out.println("Attempt to call get() on disposed BBB[1] throws exception: " + ex.getMessage());
        }

        System.out.println("AAA[2].get=" + aaa_2.get());
        System.out.println("AAA[2].increment=" + aaa_2.increment());

        System.out.println("Disposing of AAA[2]");
        aaa_2.dispose();
        try {
            System.out.println("!!! AAA[2].get=" + aaa_2.get());
        } catch (Exception ex) {
            System.out.println("Attempt to call get() on disposed AAA[2] throws exception: " + ex.getMessage());
        }
    }
}
