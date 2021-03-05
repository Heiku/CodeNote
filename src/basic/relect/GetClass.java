package basic.relect;

/**
 * @Author: Heiku
 * @Date: 2019/7/14
 */
public class GetClass {

    public static void main(String[] args) throws Exception {

        // 1. Object.getClass()
        Boolean carson = true;
        Class<?> classType = carson.getClass();
        System.out.println(classType);      // class java.lang.Boolean


        // 2. T.class
        Class<?> cy2 = Boolean.class;
        System.out.println(cy2);

        // 3.static method  Class.forName()
        Class<?> cy3 = Class.forName("java.lang.Boolean");
        System.out.println(cy3);

        // 4.Type
        Class<?> cy4 = Boolean.TYPE;
        System.out.println(cy4);
    }
}
