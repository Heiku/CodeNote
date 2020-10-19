package Basic.base;

/**
 * @Author: Heiku
 * @Date: 2019/12/19
 */
public class StringTest {
    public static void main(String[] args) {
        final String a = "hello";
        // StringTable("hello" -> string)
        String b = "hello";
        // 直接引用 string
        String result = "hello2";
        // StringTable("hello" -> string, "hello2" -> string)

        String c = a + 2;
        // 这里 c 会经过编译器优化，直接找到 result 对应的引用，赋值给 局部变量表中 c
        String d = b + 2;
        // 而这里是采用 StringBuilder#append，最后通过 toString() 得到一个新的 string


        /**
         * 编译器结果： 建议采用 javap 查看
         *
         * String a = "hello";
         *         String b = "hello";
         *         String result = "hello2";
         *         String c = "hello2";
         *         String d = b + 2;
         */

        // a.intern() 返回第一个 stringTable 中第一个的引用\


    }
}
