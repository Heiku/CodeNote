package Basic.base;

/**
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

    }
}
