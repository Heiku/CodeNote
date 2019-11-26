package Basic.type.erase;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Heiku
 * @Date: 2019/7/15
 *
 *
 * 讲讲关于Java的 “假”泛型？
 *
 *      Java的泛型只是一个语法糖，采用类型擦除，用于编译期间确定类型，而不像c++的真泛型，可以通过模板T去调用实际的方法
 *
 */
public class ErasedTypeEquivalence {
    public static void main(String[] args) throws Exception {

        // demo1
        List<Integer> list = new ArrayList<>();
        list.add(1);
        // 这里这样加，再编译期间是不允许的，所以采用反射的方法构建
        // list.add("a");

        Method method = list.getClass().getMethod("add", Object.class);
        method.invoke(list, "a");

        System.out.println(list);
        System.out.println(list.get(1));

        // [1, a]
        // a


        // demo2
        Class c1 = new ArrayList<Integer>().getClass();
        Class c2 = new ArrayList<String>().getClass();
        System.out.println(c1 == c2);

        // true


    }
}
