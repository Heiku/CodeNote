package Basic.lock.synchronize;

/**
 * @Author: Heiku
 * @Date: 2019/12/11
 */
public class BiasedLock {
    private static Object lock = new Object();

    public static void main(String[] args) {
        // set main thread id on BiasedLock.class head and frame stack
        method1();
        // next time when main thread call method2(), check BiasedLock.class head, and continue
        method2();
    }
    synchronized static void method1(){}
    synchronized static void method2(){}
}
