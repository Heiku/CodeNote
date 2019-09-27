package Baisc.future;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static java.lang.String.format;

/**
 * @Author: Heiku
 * @Date: 2019/9/26
 */
public class AsynchronousExceptionsHandlingWithHandle {

    public static void main(final String[] args) throws InterruptedException, ExecutionException {
        for (final boolean failure : new boolean[]{false, true}) {

            CompletableFuture<Integer> x = CompletableFuture.supplyAsync(() -> {
                if (failure) {
                    throw new RuntimeException("Oops, something went wrong");
                }
                return 42;
            });


            // handle 对上游的 CompletableFuture 进行第二次处理，
            // 与 whenComplete() 不同的是，可以进行返回值，对结果重新构造进行返回
            CompletableFuture<HttpResponse> tryX = x.handle((value, ex) -> { // Note that tryX and x are of different type.
                if (value != null) {
                    // We get a chance to transform the result...
                    return new HttpResponse(200, format("{\"value\": %s}", value));
                } else {
                    // ... or return details on the error using the ExecutionException's message:
                    return new HttpResponse(500, format("{\"error\": \"%s\"}", ex.getMessage()));
                }
            });

            // Blocks (avoid this in production code!), and either returns the promise's value:
            System.out.println(tryX.get());
            System.out.println("isCompletedExceptionally = " + tryX.isCompletedExceptionally());

            // Output[failure=false]: 200 - {"value": 42}
            // Output[failure=false]: isCompletedExceptionally = false

            // Output[failure=true]: 500 - {"error": "java.lang.RuntimeException: Oops, something went wrong"}
            // Output[failure=true]: isCompletedExceptionally = false
        }
    }

    private static class HttpResponse {
        private final int status;
        private final String body;

        public HttpResponse(final int status, final String body) {
            this.status = status;
            this.body = body;
        }
        public String toString() { return status + " - " + body; }
    }
}
