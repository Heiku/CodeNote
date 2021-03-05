package basic.reference;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

/**
 * 软引用（SoftReference）：用来描述有用但非必须的对象
 *
 *      对于软引用关联的对象，在系统要发生内存溢出之前，将会把这些对象列进回收范围之内，进行第二次回收，如果这次回收还没有足够的内存，才会抛出
 *      内存溢出异常
 *
 * -Xmx5m
 *
 *
 * @Author: Heiku
 * @Date: 2019/11/5
 */
public class SoftReferenceTest {

    private static ReferenceQueue<BaseReference> queue = new ReferenceQueue<>();

    public static void main(String[] args) throws Exception {

        BaseReference reference = new BaseReference();
        SoftReference<BaseReference> softRef = new SoftReference(reference, queue);

        // 试图找到引用信息
        new Thread(new CheckReference()).start();

        // 将对象设置为 null，建议触发 gc
        reference = null;
        System.gc();

        // 这里没有被回收，因为 softRef 只在内存不足的时候才会被回收
        System.out.println("After GC: Soft Get = " + softRef.get());
        System.out.println("allocate big memory");

        // 分配内存空间，导致 oom，这个时候，softRef 就被回收了
        byte[] b = new byte[5 * 1024 * 790];
        System.out.println("After new byte[]: Soft Get = " + softRef.get());
    }

    public static class CheckReference implements Runnable{

        Reference<BaseReference> ref = null;

        @Override
        public void run() {
            try {
                System.out.println("start remove reference queue");
                ref = (Reference<BaseReference>) queue.remove();

                System.out.println("end remove reference queue");
            }catch (InterruptedException e){
                e.printStackTrace();
            }

            if (ref != null){
                System.out.println("Object for softReference is " + ref.get());
            }
        }
    }
}
