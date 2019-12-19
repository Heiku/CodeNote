package Basic.base;

/**
 * @Author: Heiku
 * @Date: 2019/12/19
 */
public class StringTest {
    public static void main(String[] args) {
        //          0: ldc           #2                  // String hello
        //         2: astore_1
        //         3: ldc           #3                  // String world
        //         5: astore_2
        //         6: new           #4                  // class java/lang/String
        //         9: dup
        //        10: ldc           #5                  // String bad girl
        //        12: invokespecial #6                  // Method java/lang/String."<init>":(Ljava/lang/String;)V
        //        15: astore_3
        //        16: new           #7                  // class java/lang/StringBuilder
        //        19: dup
        //        20: invokespecial #8                  // Method java/lang/StringBuilder."<init>":()V
        //        23: aload_1
        //        24: invokevirtual #9                  // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        //        27: aload_2
        //        28: invokevirtual #9                  // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        //        31: invokevirtual #10                 // Method java/lang/StringBuilder.toString:()Ljava/lang/String;
        //        34: astore        4
        String a = "hello";
        String b = "world";
        String d = "bad girl";

        // in compiling, compiler will translate " + " to StringBuilder for connecting two string
        String hello = a + b;
    }
}
