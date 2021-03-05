package basic.lock;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author: Heiku
 * @Date: 2019/11/28
 */
public class LockCostTest {

    // 2 ms
    @Test
    public void noLockIncr(){
        long start = System.currentTimeMillis();
        int n = 0;
        for (int i = 0; i < 1000000000; i++) {
            n++;
        }
        System.out.println(System.currentTimeMillis() - start);
    }

    // 5383 ms
    @Test
    public void lockIncr(){
        long start = System.currentTimeMillis();
        AtomicInteger n = new AtomicInteger(0);
        for (int i = 0; i < 1000000000; i++) {
            n.incrementAndGet();
        }
        System.out.println(System.currentTimeMillis() - start);
    }



}
