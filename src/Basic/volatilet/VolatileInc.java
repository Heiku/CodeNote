package Basic.volatilet;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * volatile++ -> getValue + incr + setValue, is not atomic
 *
 *
 * @Author: Heiku
 * @Date: 2019/11/25
 */
public class VolatileInc implements Runnable {

    //private static volatile int count = 0;
    private static AtomicInteger count = new AtomicInteger(0);

    @Override
    public void run() {
        for (int i = 0; i < 10000; i++) {
            //count++;
            count.incrementAndGet();
        }
    }

    public static void main(String[] args) throws Exception {
        VolatileInc v = new VolatileInc();
        Thread t1 = new Thread(v, "thread A");
        Thread t2 = new Thread(v, "thread B");

        t1.start();
        t2.start();

        Thread.sleep(2000);
        for (int i = 0; i < 10000; i++){
            //count++;
            count.incrementAndGet();
        }

        System.out.println("final count: " + count);
    }
}
