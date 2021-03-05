package basic.future;

import java.util.concurrent.CompletableFuture;
/**
 * Complete
 *
 *
 * @Author: Heiku
 * @Date: 2019/9/26
 */
public class AsynchronousExceptions {
    public static void main(String[] args) {

        for (final boolean failure : new boolean[]{false, true}) {

            CompletableFuture<Integer> x = CompletableFuture.supplyAsync(() -> {
                if (failure) {
                    throw new RuntimeException("Oops, something went wrong");
                }
                return 42;
            });


            // 只有当 CompletableFuture 抛出异常的时候，才会触发这个 exceptionally的计算，调用 function计算值
            // 类似于 try-catch，可以用于捕获异常并返回一个默认的值或者是错误代码
            try {
                // x.get()，如果在异步执行总出现异常，那就会显式抛出，被catch
                // x.isCompletedExceptionally()，这里记录了这段 future中是否出现异常的情况
                System.out.println(x.get());
                System.out.println("isCompletedExceptionally = " + x.isCompletedExceptionally());
                // Output[failure=false]: 42
                // Output[failure=false]: isCompletedExceptionally = false
            } catch (Exception e) {
                // ... rethrows the RuntimeException wrapped as an ExecutionException
                System.out.println(e.getMessage());
                System.out.println(e.getCause().getMessage());
                System.out.println("isCompletedExceptionally = " + x.isCompletedExceptionally());
                // Output[failure=true]: java.lang.RuntimeException: Oops, something went wrong
                // Output[failure=true]: Oops, something went wrong
                // Output[failure=true]: isCompletedExceptionally = true
            }
        }
    }
}
