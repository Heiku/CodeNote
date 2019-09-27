package Baisc.future;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

/**
 * @Author: Heiku
 * @Date: 2019/9/18
 */
public class CompletableFutureDemo2 {
    public static void main(String[] args) throws InterruptedException {
        long l = System.currentTimeMillis();

        // 开始异步调用
        CompletableFuture<Integer> completableFuture = CompletableFuture.supplyAsync(() -> {
            System.out.println("在回调中执行耗时操作...");
            timeConsumingOperation();
            return 100;
        });

        // 在异步调用中在进行回调，thenCompose 相当于重新开启一个异步线程去执行，会等到上一个结果完成后，执行当前回调
        completableFuture = completableFuture.thenCompose(i -> {
            return CompletableFuture.supplyAsync(() -> {
                System.out.println("在回调的回调中执行耗时操作...");
                timeConsumingOperation();
                System.out.println("回调中的回调执行时间(这里 2 个sleep 3s):" + (System.currentTimeMillis() - l) + " ms");
                return i + 100;
            });
        });

        // 最后进行调用计算总结果
        completableFuture.whenComplete((result, e) -> {
            System.out.println("计算结果:" + result);
        });
        System.out.println("主线程运算耗时:" + (System.currentTimeMillis() - l) + " ms");
        new CountDownLatch(1).await();
    }

    static void timeConsumingOperation() {
        try {
            Thread.sleep(3000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
