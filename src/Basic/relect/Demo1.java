package Basic.relect;

import java.lang.reflect.Field;

/**
 * @Author: Heiku
 * @Date: 2019/7/14
 */
public class Demo1 {

    public static void main(String[] args) throws Exception{

        Class<?> clazz = Student.class;

        // class对象创建实例
        Object stu = (Student) clazz.newInstance();

        // 通过Class对象获取成员变量 name
        Field f = clazz.getDeclaredField("name");

        // 设置私有属性访问权限
        f.setAccessible(true);

        // 设置属性值
        f.set(stu, "Da Vinci");

        System.out.println(f.get(stu));
        System.out.println(((Student) stu).getName());


        /*Method []methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            System.out.println(method.getName());
        }*/
    }
}
