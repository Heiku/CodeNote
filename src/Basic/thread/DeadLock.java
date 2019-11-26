package Basic.thread;


    /**
     *  jps：用于查看对应的Java进程的PID，
     *
     *      jps -v : 显示虚拟机参数
     *      jps -m : 传递给main()函数的参数
     *      jps -l : 显示主类的全路径
     *
     *
     *  jstack： 用户生成Java虚拟机当前的线程快照（可以分析死锁）
     *
     *      主要是查看线程的状态，比如线程的lock，waiting，running等状态
     *
     *
     *  jmap：用于打印Java线程的共享对象内存映射或对内存细节
     *
     *      jmap -heap pid : 查看各个堆空间（new, old, prem）的使用占用情况（capacity, used, free 等比例参数）
     *      jmap -histo pid : 查看堆内存空间中的对象数量与大小
     *
     *  jstat：用于监控虚拟机各种运行状态信息的命令行工具
     *
     *      jstat -class pid : 显示加载的class数量
     *      jstat -compiler pid : 显示编译的数量
     *      jstat -gcutil pid : 显示gc的信息
     *
     *  jhat：jmap可以生成Java堆的Dump文件，再通过jhat可以映射位html文件，展示在网页上
     *
     */

    public class DeadLock implements Runnable {

        // flag=1，占有对象o1，等待对象o2
        // flag=0，占有对象o2，等待对象o1
        public int flag = 1;

        // 定义两个Object对象，模拟两个线程占有的资源
        public static Object o1 = new Object();
        public static Object o2 = new Object();

        public static void main(String[] args) {

            DeadLock deadLock1 = new DeadLock();
            DeadLock deadLock2 = new DeadLock();

            deadLock1.flag = 0;
            deadLock2.flag = 1;

            Thread thread1 = new Thread(deadLock1);
            Thread thread2 = new Thread(deadLock2);

            thread1.start();
            thread2.start();
        }

        public void run() {

            System.out.println("flag: " + flag);

            // deadLock2占用资源o1，准备获取资源o2
            if (flag == 1) {
                synchronized (o1) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    synchronized (o2) {
                        System.out.println("1");
                    }
                }
            }

            // deadLock1占用资源o2，准备获取资源o1
            else if (flag == 0) {
                synchronized (o2) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    synchronized (o1) {
                        System.out.println("0");
                    }
                }
            }
        }
    }