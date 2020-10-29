package Basic.base;

/**
 * https://juejin.im/post/6844904191324864520
 *
 * @Author: Heiku
 * @Date: 2020/10/28
 */
public class StringFinalTest {
    public static void main(String[] args) {
        // 在堆中创建两个对象
        // 1. 第一个字符对象，然后在运行方法区的字符串常量表中 StringTable "1" -> 这个对象
        // 2. 第二个为 new String 构建出来的对象
        // 所以这就解释了 s1 != s3

        // 然后 s2 = s1.intern() 从 StringTable 里找 equals 相同的字符串，然后复制引用（也就是第一个字符对象）
        String s1 = "11";
        String s2 = s1.intern();
        String s3 = "11";

        System.out.println(s1 == s3);
        System.out.println(s2 == s3);

        // 这里会构建 stringBuilder + 2个new String + 字符常量池中的"1" 4个对象
        // 然后通过 new String(arr, offset, count), 注意 这里没有构建 "11" 并放入到字符串常量池中 (第5个对象)
        // 然后 intern() 将 string(“11”) 构建放入到常量池中 "11" 并指向 string("11")
        // 最后 s5 = "11" 根据 stringTable 复制 string("11") 的引用
        // 所以 s4 = s5
        String s4 = "1" + "1";
        s4.intern();
        String s5 = "11";
        System.out.println(s4 == s5);

        // 注意，StringBuilder.toString() 是通过 new String(arr, offset, count) 而不是 new String(String..)
        char[] arr = new char[]{'1', '1'};
        String s6 = new String(arr, 0, arr.length);
        s6.intern();
        String s7 = "11";
        System.out.println(s6 == s7);
    }
}
