package Others;

public class BinUtils {


    /**
     *  jps ：用于查看对应的Java进程的PID，
     *
     *      jps -v : 显示虚拟机参数
     *      jps -m : 传递给main()函数的参数
     *      jps -l : 显示主类的全路径
     *
     *
     *  jstack ： 用户生成Java虚拟机当前的线程快照（可以分析死锁）
     *
     *
     *
     */

    public static void main(String[] args) {
        Thread thread1 = new Thread(new DeadLockClass(true));
        Thread thread2 = new Thread(new DeadLockClass(false));

        thread1.start();
        thread2.start();
    }


    class DeadLockClass implements Runnable{
        public boolean flag;

        DeadLockClass(boolean flag){
            this.flag = flag;
        }


        @Override
        public void run() {
            if (flag){
                while (true){
                    synchronized (Suo.o1){
                        System.out.println("o1 " + Thread.currentThread().getName());

                        synchronized (Suo.o2){
                            System.out.println("o2 " + Thread.currentThread().getName());
                        }
                    }
                }
            }else {
                while (true){
                    synchronized (Suo.o2){
                        System.out.println("o2 " + Thread.currentThread().getName());

                        synchronized (Suo.o1){
                            System.out.println("o1 " + Thread.currentThread().getName());
                        }
                    }
                }
            }
        }
    }
}
class Suo{
    static Object o1 = new Object();
    static Object o2 = new Object();
}