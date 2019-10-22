package Baisc.thread;

/**
 *
 * join():
 *      语义：将几个并行的线程合并为一个单线程执行。
 *      应用场景：当一个线程必须等待另一个线程执行完毕时，才能执行
 *
 *  Waits for this thread to die. 即等到当前线程执行到结束的时候，后续才继续执行
 *
 *  原理：底层是通过条用 Object 的 wait() 方法
 *
 * @Author: Heiku
 * @Date: 2019/10/21
 */
public class ThreadJoinTest {

    public static void main(String[] args) {
        for (int i = 0; i < 5; i++){
            MyThread t = new MyThread();
            t.start();

            try {
                // 这里主线程 main 获得了 线程t对象的锁，并在 t.join()里执行了 t.wait()，即挂起了主线程
                // 线程对象t 的 notify() 是在 JVM 源码中的 JavaThread::exit() 中的lock.notify_all(thread) 施放对象t的锁
                // 这样 main线程就可以继续执行
                // 原则：谁运行 t.join() 就阻塞谁
                t.join();
            }catch (InterruptedException e){
                e.printStackTrace();
            }

            System.out.println("主线程执行完毕");
            System.out.println("~~~~~~~~~~~~");
        }
    }


    static class MyThread extends Thread{
        @Override
        public void run() {
            System.out.println("子线程执行完毕");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}