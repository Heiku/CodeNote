package Baisc.relect;

/**
 * @Author: Heiku
 * @Date: 2019/7/14
 */
public class Demo2 {

    public static void main(String[] args) throws Exception {
        Class<?> clazz = Student.class;

        Object obj1 = clazz.getConstructor().newInstance();

        Object obj2 = clazz.getConstructor(String.class).newInstance("Da Vinci");

    }
}
