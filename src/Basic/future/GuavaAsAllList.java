package Basic.future;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

/**
 * 默认情况下，allOf 会等在所有的任务完成，即使其中有一个失败，也不会影响其他任务继续执行，
 * 但我们通常会任务，一个任务的结束就意味着整体任务的失败，继续执行其他任务的意义不大。
 *
 * 类似于 Guava 的 allAsList,
 *
 *  allAsList(Iterable<ListenableFuture>): Returns a ListenableFuture whose value is a list containing the values of each of the input futures,
 *  in order. If any of the input futures fails or is cancelled, this future fails or is cancelled.
 *
 *
 *
 *
 *
 * @Author: Heiku
 * @Date: 2019/9/26
 */
public class GuavaAsAllList {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
            System.out.println("-- future1 -->");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            System.out.println("<-- future1 --");
            return "Hello";
        });


        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            System.out.println("-- future2 -->");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            System.out.println("<-- future2 --");
            throw new RuntimeException("Oops!");
        });

        CompletableFuture<String> future3 = CompletableFuture.supplyAsync(() -> {
            System.out.println("-- future3 -->");
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            System.out.println("<-- future3 --");
            return "world";
        });

        // CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(future1, future2, future3);
        // combinedFuture.join();


        // future2 出现异常后，会导致后面的 future3不会继续进行
        CompletableFuture<Void> allWithFailFast = CompletableFuture.allOf(future1, future2, future3);
        Stream.of(future1, future2, future3).forEach(f -> f.exceptionally(e -> {
            allWithFailFast.completeExceptionally(e);
            return null;
        }));

        allWithFailFast.join();

    }
}
