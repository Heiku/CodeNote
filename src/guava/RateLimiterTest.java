package guava;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.RateLimiter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;

/**
 * @Author: Heiku
 * @Date: 2020/1/14
 */
public class RateLimiterTest {
    public static void main(String[] args) {
        ListeningExecutorService executor = MoreExecutors.listeningDecorator(
                Executors.newFixedThreadPool(100)
        );

        // put 1 token every sec
        RateLimiter limiter = RateLimiter.create(1);
        for (int i = 0; i < 50; i++) {
            Double acquire = null;

            if (i == 1) {
                acquire = limiter.acquire(1);
            } else if (i == 2) {
                acquire = limiter.acquire(10);
            } else if (i == 3) {
                acquire = limiter.acquire(2);
            } else if (i == 4) {
                acquire = limiter.acquire(20);
            } else {
                acquire = limiter.acquire(2);
            }
            executor.submit(new Task("acquire successfully, acquire " + acquire + " No." + i + " task"));
        }
    }
}


class Task implements Runnable {
    String str;

    public Task(String str) {
        this.str = str;
    }

    @Override
    public void run() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        System.out.println(sdf.format(new Date()) + " | " + Thread.currentThread().getName() + str);
    }
}