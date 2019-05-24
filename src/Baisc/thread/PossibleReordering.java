package Baisc.thread;

/**
 * @Author: Heiku
 * @Date: 2019/5/24
 *
 * 关于内存重排序的研究
 *      处理器：指令乱序执行 (out-of-order execution)， 条件允许的情况下，直接运行当前有能力立即执行的后续指令，避开下一条指令所需数据的等待时间，
 *            通过乱序执行，处理器可大大提高执行效率
 *      Java运行时环境：JIT 编译器也会做指令重排序，即生成的机器指令与字节码指令的顺序不一致。
 *
 *
 * as - if - serial语义：
 *      所有的动作(Action) 都可以为了优化而重排序，但必须保证它们重排序后的结果和程序代码本身的应有co结果是一致的。
 *      Java 编译器， 运行时环境（JRE）和 处理器都会保证单线程下的 as - if - serial语义。
 *
 *      e.g. int a = 1;
 *           int b = 2;
 *           int c = b + a;
 *
 *           可分为以下几个步骤：
 *                   1. 对a赋值1
 *                   2. 对b赋值2
 *                   3. 取a的值
 *                   4. 取b的值
 *                   5. 将取到两个值相加后存入c
 *
 *           因为存在数据依赖，所以 （1，3，5） （2，4，5） 不能重排，否则无法保证 as-if-serial 语义
 *
 *
 * Java 的内存模型：（by JSR-133） 提供统一的可参考规范，屏蔽平台差异
 *      happens-before：前后两个操作不会重排序，且后者对前者可见
 *              程序次序：线程中的每个动作A都happens-before于该线程中的每一个动作B，其中，在程序中，所有的动作B都能出现在A之后。
 *              监视器锁：对一个监视器锁的加锁 happens-before于每一个后续对同一监视器锁的解锁。
 *              volatile变量：对volatile域的写入操作happens-before于每一个后续对同一个域的读写操作。
 *              线程启动：在一个线程里，对Thread.start的调用会happens-before于每个启动线程的动作。
 *              线程终结：线程中的任何动作都happens-before于其他线程检测到这个线程已经终结、或者从Thread.join调用中成功返回，或Thread.isAlive返回false。
 *              中断法则：一个线程调用另一个线程的interrupt happens-before于被中断的线程发现中断。
 *              终结法则：一个对象的构造函数的结束happens-before于这个对象finalizer的开始。
 *              传递性：如果A happens-before于B，且B happens-before于C，则A happens-before于C
 *
 * Java 内存模型（JMM）对 volatile 和 final 做了扩展：
 *      volatile：读取volatile不会发生重排序，通过内存屏障（Memory Barrier）实现
 *      final：保证了一个对象的构建方法结束前，所有的final成员变量必须完成初始化
 *
 *
 * 内存屏障（Memory Barrier）：一种CPU指令，用于控制特定条件下的重排序和内存可见性问题。
 *      Java编译器也会根据内存屏障的规则禁止重排序。
 *
 *      LoadLoad屏障：对于这样的语句Load1; LoadLoad; Load2，在Load2及后续读取操作要读取的数据被访问前，保证Load1要读取的数据被读取完毕。
 *      StoreStore屏障：对于这样的语句Store1; StoreStore; Store2，在Store2及后续写入操作执行前，保证Store1的写入操作对其它处理器可见。
 *      LoadStore屏障：对于这样的语句Load1; LoadStore; Store2，在Store2及后续写入操作被刷出前，保证Load1要读取的数据被读取完毕。
 *      StoreLoad屏障：对于这样的语句Store1; StoreLoad; Load2，在Load2及后续所有读取操作执行前，保证Store1的写入对所有处理器可见。
 */
public class PossibleReordering {

    static int x = 0, y = 0;
    static int a = 0, b = 0;

    public static void main(String[] args) throws Exception {

        /*Thread one = new Thread(new Runnable() {
            @Override
            public void run() {
                a = 1;
                x = b;
            }
        });

        Thread two = new Thread(new Runnable() {
            @Override
            public void run() {
                b = 1;
                y = a;
            }
        });

        one.start();
        two.start();
        // main thread 等待 one,two 两个线程完全结束才向下执行
        one.join();
        two.join();

        // 可能出现结果 (1,0) (0,1) (1,1)
        // (0, 0) 发生重排序即 x = b, y = a
        System.out.println("(" + x + "," + y + ")");*/


        int x, y;
        x = 1;
        try {
            // y = 0 / 0 可能会被重排序在 x = 2 之前执行，
            // 但为了不至于输出 x = 1 的结果，JIT 在重排序时会在catch中插入错误补偿代码，将x = 2，将程序恢复到异常应有的状态

            x = 2;
            y = 0 / 0;
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            System.out.println("x = " + x);
        }
    }
}
