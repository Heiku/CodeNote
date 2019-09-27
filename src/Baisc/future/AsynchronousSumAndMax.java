package Baisc.future;

import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @Author: Heiku
 * @Date: 2019/9/26
 */
public class AsynchronousSumAndMax {
    public static void main(final String[] args) {
        stopwatch(() -> {
            Stream<CompletableFuture<Integer>> xs = Stream.of(1, 2, 3, 4, 5).map(
                    x -> CompletableFuture.supplyAsync(() -> compute(x))
            );

            CompletableFuture<Integer> sum = xs.reduce(completedFuture(0), (x, y) -> x.thenCombine(y, (i, j) -> i + j));

            CompletableFuture<Integer>[] ys = Stream.of(1, 2, 3).map(
                    x -> sum.thenApplyAsync(s -> multiply(s, x))
            ).toArray(CompletableFuture[]::new);

            CompletableFuture<Integer> max = CompletableFuture.allOf(ys).thenApply(
                    (Void) -> Arrays.stream(ys)
                            .map(y -> y.getNow(Integer.MAX_VALUE)) // Guaranteed to return y's value, given we synchronise with allOf, and only thenApply this function.
                            .max(Comparator.naturalOrder())
                            .get()
            );

            // Block and wait for results (avoid this in production code!):
            try {
                println("Result: " + max.get());
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }

            // Output:
            // [ForkJoinPool.commonPool-worker-1]: Computing 1...
            // [ForkJoinPool.commonPool-worker-2]: Computing 2...
            // [ForkJoinPool.commonPool-worker-4]: Computing 4...
            // [ForkJoinPool.commonPool-worker-3]: Computing 3...
            // [ForkJoinPool.commonPool-worker-5]: Computing 5...
            // [ForkJoinPool.commonPool-worker-1]: Computed 1.
            // [ForkJoinPool.commonPool-worker-5]: Computed 5.
            // [ForkJoinPool.commonPool-worker-4]: Computed 4.
            // [ForkJoinPool.commonPool-worker-2]: Computed 2.
            // [ForkJoinPool.commonPool-worker-3]: Computed 3.
            // [ForkJoinPool.commonPool-worker-2]: Computing 15 * 1...
            // [ForkJoinPool.commonPool-worker-4]: Computing 15 * 2...
            // [ForkJoinPool.commonPool-worker-5]: Computing 15 * 3...
            // [ForkJoinPool.commonPool-worker-4]: Computed 15 * 2 = 30.
            // [ForkJoinPool.commonPool-worker-5]: Computed 15 * 3 = 45.
            // [ForkJoinPool.commonPool-worker-2]: Computed 15 * 1 = 15.
            // [main]: Result: 45
            // Elapsed time: 4 seconds.
        });
    }

    private static int compute(final int x) {
        println("Computing " + x + "...");
        sleep(2, SECONDS);
        println("Computed " + x + ".");
        return x;
    }

    private static int multiply(final int x, final int y) {
        println("Computing " + x + " * " + y + "...");
        sleep(2, SECONDS);
        final int r = x * y;
        println("Computed " + x + " * " + y + " = " + r + ".");
        return r;
    }

    private static void sleep(final int duration, final TimeUnit unit) {
        try { unit.sleep(duration); } catch (InterruptedException e) { e.printStackTrace(); }
    }

    private static void println(final String message) {
        System.out.println("[" + Thread.currentThread().getName() + "]: " + message);
    }

    private static void stopwatch(final Runnable action) {
        final long begin = System.currentTimeMillis();
        action.run();
        System.out.println("Elapsed time: " + (System.currentTimeMillis() - begin) / 1000 + " seconds.");
    }
}