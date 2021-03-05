package basic.executors;

import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Author: Heiku
 * @Date: 2019/11/25
 */
public class SafelyQuitExecutors {
    public static void main(String[] args) throws Exception {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                2, 2, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100),
                new ThreadPoolExecutor.AbortPolicy()
        );

        long start = System.currentTimeMillis();
        for (int i = 0; i <= 100; i++) {
            pool.execute(new Job(i));
        }

        // running -> shutdown, stop receive job,but continue doing the work from queue     -> tidying -> terminated
        // shutdownNow(): running -> stop, stop receive job, and stop doing work            -> tidying -> terminated
        pool.shutdown();

        // wait to terminated status
        while (!pool.awaitTermination(1, TimeUnit.SECONDS)) {
            System.out.println("worker still working ...");
        }

        long end = System.currentTimeMillis();
        System.out.println("closing threadPoll costs: " + (end - start) + " ms");
    }
}


class Job implements Runnable {

    private int id;

    Job(int id) {
        this.id = id;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(new Random().nextInt(3) * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("finish job - " + id);
    }
}