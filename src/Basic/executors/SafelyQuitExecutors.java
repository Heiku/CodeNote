package Basic.executors;

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
            pool.execute(new Job());
        }

        // running -> shutdown, stop receive job,but continue doing the work from queue     -> tidying -> terminated
        // shutdownNow(): running -> stop, stop receive job, and stop doing work            -> tidying -> terminated
        pool.shutdown();

        // wait to terminated status
        while (!pool.awaitTermination(1, TimeUnit.MILLISECONDS)){
            System.out.println("worker still working ...");
        }

        long end = System.currentTimeMillis();
        System.out.println("closing threadPoll costs: " + (end - start) + " ms");
    }
}


class Job implements Runnable{
    @Override
    public void run() {
        System.out.println("do my job");
    }
}