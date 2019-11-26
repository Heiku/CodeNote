package Basic.reference;

/**
 * {@link java.lang.ref.Finalizer}
 *
 *
 *  Final Reference:
 *
 *      1.当前类或父类中含有一个参数为空，返回值为 void 的 finalize()
 *      2.并且该 finalize() 方法体非空
 *
 * 满足以上两点的叫做 f类，在运行期间，会被 JVM 注册到 java.lang.ref.Finalizer 的 referenceQueue 中
 *
 *
 * GC 回收问题：
 *
 *      1.对象因为被 Finalizer 引用从而变成一个临时的强引用，因此无法立即被回收
 *      2.对象至少经历两次 GC 才能被释放，因为只有在 FinalizerThread 执行完 f对象的 finalize() 的情况下，才有可能被 GC 回收，
 *          而这个期间可能经历了多次 GC，但是一直没有执行对象的 finalize()
 *      3.CPU 资源比较稀缺的情况下，FinalizerThread 有可能因为优先级比较低而延迟执行对象的 finalize()
 *      4.因为对象的 finalize() 迟迟没有进行执行，有可能导致大部分 f() 对象进入到 old分代，此时容易引发 old分代的 GC，甚至 Full GC，
 *          GC暂停的时间明显变长，甚至 OOM
 *      5.f对象的 finalize() 被调用后，可能因为 finalize() 中的 执行任务比较大，导致 finalize() 时间场，f对象就迟迟无法回收
 *
 *
 * @Author: Heiku
 * @Date: 2019/11/5
 */
public class FinalReferenceTest {
}
