package Baisc.reference;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.TimeUnit;

/**
 * 虚引用（PhantomReference）
 *
 *      虚引用主要用来追踪对象被垃圾回收的状态，通过查看引用队列中是否包含对象所对应的虚引用来判断它是否即将被垃圾回收，
 *      它并不被期待用来获取目标对象的引用，它的引用会被放入到一个 ReferenceQueue 对象中，从而达到跟踪对象垃圾回收的作用
 *
 *      当 phantomReference 被放入到队列时，说明 reference 的 finalize() 已经被调用了，并且垃圾回收器已经准备回收它的内存了
 *      通过 ref.get() 获取虚引用关联的对象永远为空
 *
 *
 * gc()注意：
 *
 *      1.System.gc() -> Runtime.getRuntime().gc() 这个过程中时建议 jvm 进行gc，不保证一定立刻执行
 *      2.对于已经没有地方引用的 f对象，并不会在最近的那一次 gc 里马上回收掉，而是会延迟到下一个或者下几次 gc 的时候才会被回收掉
 *          这是因为当执行的 finalize() 的动作无法在 gc() 中执行，如果 finalize() 方法执行很长的话，
 *          只能在 gc周期中，将对象重新标活，直到执行完 finalize() 并将 Final Reference 从 queue 中删除，这时候 gc才真正回收掉对象
 *
 * @Author: Heiku
 * @Date: 2019/11/5
 */
public class PhantomReferenceTest {

    private static ReferenceQueue<BaseReference> queue = new ReferenceQueue<>();

    public static void main(String[] args) throws Exception{
        BaseReference obj = new BaseReference();
        Reference<BaseReference> phantomRef = new PhantomReference<>(obj, queue);

        System.out.println("创建的虚引用为：" + phantomRef);
        new Thread(new CheckRefQueue()).start();

        obj = null;

        int i = 1;
        while (true){
            System.out.println("第 " + i++ + " 次GC");
            System.gc();
            TimeUnit.SECONDS.sleep(1);
        }
    }


    public static class CheckRefQueue implements Runnable{
        Reference<BaseReference> ref = null;

        @Override
        public void run() {
            try {
                ref = (Reference<BaseReference>) queue.remove();

                System.out.println("删除的虚引用为：" + ref + " ,获取虚引用的对象：" + ref.get());
                System.exit(0);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
