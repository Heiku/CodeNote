package Basic.lock;

import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author: Heiku
 * @Date: 2019/12/11
 */
public class FairLock implements Runnable {

    static ReentrantLock unFairLock = new ReentrantLock(false);
    static ReentrantLock fairLock = new ReentrantLock(true);

    @Override
    public void run() {
        while (true){
            try {
                fairLock.lock();
                System.out.println(Thread.currentThread().getName() + " hold lock");
            }finally {
                fairLock.unlock();
            }
        }
    }

    public static void main(String[] args) {
        FairLock fairLock = new FairLock();
        Thread t1 = new Thread(fairLock, "cxs - t1");
        Thread t2 = new Thread(fairLock, "cxs - t2");
        t1.start();
        t2.start();
    }
}
