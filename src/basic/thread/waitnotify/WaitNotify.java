package basic.thread.waitnotify;

import java.util.concurrent.TimeUnit;

/**
 * @Author: Heiku
 * @Date: 2019/12/9
 */
public class WaitNotify {
    static boolean flag = true;
    static Object lock = new Object();

    public static void main(String[] args) throws Exception {
        Thread A = new Thread(new Wait(), "wait thread");
        A.start();
        TimeUnit.SECONDS.sleep(2);
        Thread B = new Thread(new Notify(), "notify thread");
        B.start();
    }

    static class Wait implements Runnable{
        @Override
        public void run() {
            synchronized (lock){
                while (flag){
                    try {
                        System.out.println(Thread.currentThread() + " flag is true");

                        // release object lock, thread notify can get the object lock
                        lock.wait();
                    }catch (Exception e){

                    }
                }
                System.out.println(Thread.currentThread() + " flag is false");
            }
        }
    }

    static class Notify implements Runnable{
        @Override
        public void run() {
            synchronized (lock){
                flag = false;

                // notifyAll didn't release object lock(object monitor)
                // notify
                lock.notifyAll();
                try {
                    TimeUnit.SECONDS.sleep(7);
                    System.out.println("notifyAll but not release object lock");
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
}
