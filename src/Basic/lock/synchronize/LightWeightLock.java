package Basic.lock.synchronize;

/**
 * @Author: Heiku
 * @Date: 2019/12/11
 */
public class LightWeightLock implements Runnable {

    private static Object lock = new Object();

    public static void main(String[] args) {
        Thread t1 = new Thread(new LightWeightLock(), "A");
        t1.start();

        Thread t2 = new Thread(new LightWeightLock(), "B");
        t2.start();
    }

    @Override
    public void run() {
        method1();
        method2();
    }

    synchronized static void method1(){};
    synchronized static void method2(){};
}

