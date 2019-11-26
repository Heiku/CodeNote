package Basic.reference;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * 弱引用（WeakReference）：描述非必须对象
 *
 *      当程序 gc的时候，无论当前内存是否足够，都会回收只被弱引用关联的对象。
 *      一旦一个弱引用被垃圾回收器回收，便会加入到一个注册的引用队列中。
 *
 *
 * @Author: Heiku
 * @Date: 2019/11/5
 */
public class WeakReferenceTest {
    private static ReferenceQueue<BaseReference> queue = new ReferenceQueue<>();

    public static void main(String[] args) throws Exception {

        BaseReference reference = new BaseReference();
        Reference<BaseReference> weakRef = new WeakReference(reference, queue);

        // 移除 ref
        new Thread(new CheckReference()).start();

        // weakRef 在gc的时候就被回收了
        reference = null;
        System.out.println("Before GC: Weak Get = " + weakRef.get());
        System.gc();
        System.out.println("After GC: Weak Get = " + weakRef.get());

    }


    // 回收的时候会被注册到引用队列中，所以可以中引用队列中找到对应的信息
    public static class CheckReference implements Runnable{

        Reference<BaseReference> ref = null;

        @Override
        public void run() {
            try {
                ref = (Reference<BaseReference>) queue.remove();
            }catch (InterruptedException e){
                e.printStackTrace();
            }

            if (ref != null){
                System.out.println("删除的弱引用为：" + ref + "，获取到的弱引用的对象为：" + ref.get());
            }
        }
    }
}
