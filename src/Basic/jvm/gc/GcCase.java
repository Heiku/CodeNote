package Basic.jvm.gc;

/**
 * @Author: Heiku
 * @Date: 2019/12/20
 */

// -Xmx2g -Xms2g -Xmn500m -XX:+PrintGCDetails
// -XX:+UseConcMarkSweepGC -XX:+PrintHeapAtGC
public class GcCase {
    public static void main(String[] args) {
        for (int i = 0; i < 1000; i++) {
            allocate_1M();
        }
    }

    public static void allocate_1M() {
        byte[] bytes = new byte[1000 * 1024];
    }
}
