package Basic.type.generic;

/**
 * @Author: Heiku
 * @Date: 2019/7/14
 */
public class StaticFans {

    // 静态函数
    public static <T> void staticMethod(T a){
        System.out.println("StaticMethod: " + a.toString());
    }

    // 普通函数
    public <T> void ohterMethod(T a){
        System.out.println("OtherMethod: " + a.toString());
    }

    public static void main(String[] args) {
        // 静态方法
        StaticFans.staticMethod("Heiku");   // 静态泛型
        StaticFans.<Integer>staticMethod(123);  // 普通方法泛型

        // 对象方法
        StaticFans staticFans = new StaticFans();
        staticFans.ohterMethod(123);
        staticFans.<Integer>ohterMethod(123);
    }
}
