package Basic.thread;

/**
 * @Author: Heiku
 * @Date: 2019/5/18
 */
public class PrintEvenOdd {

    static final int MAX_NUM = 1000;
    static volatile int num = 0;

    public static void main(String[] args) {
        Thread t1 = new Thread(new Print());
        Thread t2 = new Thread(new Print()) ;
        t1.setName("thread-A");
        t2.setName("thread-B");
        t1.start();
        t2.start();
    }

    static class Print implements Runnable{
        @Override
        public void run() {
            try {
                while (num <= MAX_NUM){
                    // make sure just one thread can visit num
                    synchronized (PrintEvenOdd.class){
                        System.out.println(Thread.currentThread().getName()  + " print " + num++);

                        // notify thread B
                        PrintEvenOdd.class.notify();

                        // in same time, block myself (A)
                        PrintEvenOdd.class.wait();
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}



