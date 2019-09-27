package Baisc.future;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @Author: Heiku
 * @Date: 2019/9/26
 */
public class AsynchronousExceptionsHandlingWithWhenComplete {
    public static void main(final String[] args) throws InterruptedException, ExecutionException {
        for (final boolean failure : new boolean[]{false, true}) {

            CompletableFuture<Integer> x = CompletableFuture.supplyAsync(() -> {
                if (failure) {
                    throw new RuntimeException("Oops, something went wrong");
                }
                return 42;
            });


            // 使用 whenComplete 处理 CompletableFuture 运行正确与否的最后处理
            // exceptionally() 类似于 catch， whenComplete 类似于 finally
            CompletableFuture<Integer> tryX = x.whenComplete((value, ex) -> { // Note that tryX and x are of same type. This CompletableFuture acts as an invisible "decorator".
                if (value != null) {
                    // We get a chance to transform the result by adding 1...
                    System.out.println("Result: " + value);
                } else {
                    // ... or return an error value:
                    System.out.println("Error code: -1. Root cause: " + ex.getMessage());
                }
            });

            try {
                // Blocks (avoid this in production code!), and either returns the promise's value, or...
                System.out.println(tryX.get());
                System.out.println("isCompletedExceptionally = " + tryX.isCompletedExceptionally());
                // Output[failure=false]: Result: 42
                // Output[failure=false]: 42
                // Output[failure=false]: isCompletedExceptionally = false
            } catch (ExecutionException e) {
                // ... rethrows the RuntimeException wrapped as an ExecutionException
                System.out.println(e.getMessage());
                System.out.println("isCompletedExceptionally = " + tryX.isCompletedExceptionally());
                // Output[failure=true]: Error code: -1. Root cause: java.lang.RuntimeException: Oops, something went wrong
                // Output[failure=true]: Oops, something went wrong
                // Output[failure=true]: isCompletedExceptionally = true
            }
        }
    }
}
