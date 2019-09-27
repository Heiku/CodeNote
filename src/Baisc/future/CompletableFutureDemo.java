package Baisc.future;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

/**
 * @Author: Heiku
 * @Date: 2019/9/18
 */
public class CompletableFutureDemo {
    public static void main(String[] args) throws InterruptedException {
        long l = System.currentTimeMillis();

        // 未指定线程池的情况下，supplyAsync 采用的是 ForkJoinPool.commonPool 里面的线程进行执行
        CompletableFuture<Integer> completableFuture = CompletableFuture.supplyAsync(() -> {
            System.out.println("执行耗时操作...");
            timeConsumingOperation();
            return 100;
        });

        // 这里类似与对这个 future 注册了一个监听器，等到 future结果出来的时候，进行调用 whenComplete
        completableFuture.whenComplete((result, e) -> {
            System.out.println("结果：" + result);
            System.out.println("计算结果总耗时：" + (System.currentTimeMillis() - l) + " ms");
        });


        // 主线程不会受到影响，继续执行
        System.out.println("主线程运算耗时:" + (System.currentTimeMillis() - l) + " ms");

        // 这里用 countDownLatch用于查看最后结果
        new CountDownLatch(1).await();
    }


    static void timeConsumingOperation() {
        try {
            Thread.sleep(3000);
            throw new RuntimeException("eee");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
