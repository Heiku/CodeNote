package Basic.jucutils.semaphore;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * @Author: Heiku
 * @Date: 2019/12/16
 */
public class SemaphoreTest {
    private static final int COUNT = 40;

    private static Executor executor = Executors.newFixedThreadPool(COUNT);
    private static Semaphore semaphore = new Semaphore(10);

    public static void main(String[] args) {
        for (int i = 0; i < COUNT; i++) {
            executor.execute(new Task());
        }
    }

    static class Task implements Runnable {
        @Override
        public void run() {
            try {
                semaphore.acquire();
                // doSomething
                semaphore.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {

            }
        }
    }
}
