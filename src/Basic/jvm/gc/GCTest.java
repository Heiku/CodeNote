package Basic.jvm.gc;

/**
 * @Author: Heiku
 * @Date: 2019/12/18
 */
public class GCTest {

    private Object instance = null;
    private static final int _10M = 10 * 1 << 20;
    private byte[] bigSize = new byte[_10M];

    public static void main(String[] args) {
        GCTest objA = new GCTest();
        GCTest objB = new GCTest();

        objA.instance = objB;
        objB.instance = objA;

        objA = null;
        objB = null;

        System.gc();
        // [GC (System.gc())  23808K->976K(125952K), 0.0008934 secs]
        // [Full GC (System.gc())  976K->777K(125952K), 0.0050683 secs]
    }
}
