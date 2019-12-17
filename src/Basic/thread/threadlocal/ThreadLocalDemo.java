package Basic.thread.threadlocal;

import java.util.concurrent.CountDownLatch;

/**
 * @Author: Heiku
 * @Date: 2019/12/17
 */
public class ThreadLocalDemo {
    public static void main(String[] args) throws Exception {
        int THREADS = 3;
        CountDownLatch countDownLatch = new CountDownLatch(THREADS);
        InnerClass innerClass = new InnerClass();
        for (int i = 0; i < THREADS; i++) {
            new Thread(() -> {
                for (int j = 0; j < 4; j++) {
                    innerClass.add(String.valueOf(j));
                    innerClass.print();
                }
                innerClass.set("hello world");
                countDownLatch.countDown();
            }, "thread - " + i).start();
        }
        countDownLatch.await();
    }

    static class InnerClass {
        public void add(String str) {
            StringBuilder sb = Counter.counter.get();
            Counter.counter.set(sb.append(str));
        }

        public void print() {
            System.out.printf("Thread name: %s, ThreadLocal hashcode: %s, Instance hashcode: %s, Value: %s\n",
                    Thread.currentThread().getName(),
                    Counter.counter.hashCode(),
                    Counter.counter.get().hashCode(),
                    Counter.counter.get().toString());
        }

        public void set(String words) {
            Counter.counter.set(new StringBuilder(words));
            System.out.printf("Set, Thread name: %s, ThreadLocal hashcode: %s, Instance hashcode: %s, Value: %s\n",
                    Thread.currentThread().getName(),
                    Counter.counter.hashCode(),
                    Counter.counter.get().hashCode(),
                    Counter.counter.get().toString());
        }
    }


    static class Counter {
        private static ThreadLocal<StringBuilder> counter = new ThreadLocal<StringBuilder>() {
            @Override
            protected StringBuilder initialValue() {
                return new StringBuilder();
            }
        };
    }
}
