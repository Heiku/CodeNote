package basic.reference;

/**
 * 强引用，即我们平时创建的对象，一般不会被回收，如果当前的内存空间不足，那么宁愿抛出 OutOfMemoryError，也不会回收
 *
 * 特点：
 *      1.可直接访问目标对象
 *      2.强引用所指向的对象在任何时候都不会被系统回收
 *      3.强引用可能导致内存泄漏，请注意对象的释放回收
 *
 *
 *
 *
 * @Author: Heiku
 * @Date: 2019/11/5
 */
public class StrongReferenceTest {

    public static void main(String[] args) {
        while (true){
            new BaseReference();
        }
    }
}
