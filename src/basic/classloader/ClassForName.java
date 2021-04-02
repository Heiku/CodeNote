package basic.classloader;

/**
 * @Author: Heiku
 * @Date: 2019/11/26
 */
public class ClassForName {
    private static String staticFiled = staticMethod();
    static {
        System.out.println("invoke static block");
    }


    public static String staticMethod(){
        System.out.println("invoke static method");
        return "static";
    }
}
