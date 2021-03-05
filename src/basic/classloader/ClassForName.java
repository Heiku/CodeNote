package basic.classloader;

/**
 * @Author: Heiku
 * @Date: 2019/11/26
 */
public class ClassForName {

    static {
        System.out.println("invoke static block");
    }

    private static String staticFiled = staticMethod();

    public static String staticMethod(){
        System.out.println("invoke static method");
        return "static";
    }
}
