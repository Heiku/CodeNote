package Others;

import java.util.ArrayList;
import java.util.List;

/**
 *  对与基本数据类型的总结
 *
 *  byte short int long double float char boolean
 *
 *  Byte Short Integer Long Double FLoat Character Boolean
 *
 *
 *  问题：为什么需要基本类型？
 *
 *      因为在Java中的对象基本上都是在堆上内配，然后通过栈中的引用去使用这些对象，所以，对象本身是比较占资源的。
 *      采用基本类型的方式，对象直接在栈中创建，效率更高。
 *
 *
 *      那为什么需要包装类呢？
 *
 *      这是为了满足Java面对对象的需要，因为所有对象的基类都是Object，而当我们要以操作对象的方式去操作基本数据时，这是就需要包装类了
 *    （Wrapper Class），作为基本数据的抽象对象去使用它。特别时当我们在使用容器的时候，List<Object>
 *
 *
 *      拆箱装箱：既然有了基本类型和包装类，当我们需要对其进行数据间的转化时，就衍生出 拆箱装箱这种概念。
 *              同时要注意缓存，装箱拆箱的效率，==，equals()等问题。
 *
 * @Author JiaHong Lin
 * @Date 2018-12-17
 */
public class BasicClass {

    public static void main(String[] args) {
        int i = Integer.MAX_VALUE;
        int j = Integer.MAX_VALUE;

        int k = i + j;

        /**
         * i(2147483647) + j(2147483647) = k(-2)
         *
         * 注意， 这里出现了数据溢出，但却不会抛出异常，所以在开发中要注意数据的大小判断
         */
        System.out.println("i(" + i + ") + j(" + j + ") = " + "k(" + k + ")");


        Integer a = 10;     // 自动装箱
        int b = a;          // 自动拆箱


        /**
         * 放入集合类
         */
        List<Integer> intList = new ArrayList<>();
        for (int v = 0; v < 10; v++){
            intList.add(v);             // == intList.add(Integer.valueOf(v));   放入集合的时候，自动装箱
        }


        /**
         * 运算
         */
        Integer x1 = 2;     // Integer x1 = Integer.valueOf(x1);
        Integer x2 = 4;     // Integer x2 = Integer.valueOf(x2);
        System.out.println(x1 + x2);    // sout( x1.intValue() + x2.intValue())    计算的时候实际上时采用基本类型去计算


        /**
         * Integer缓存
         *
         * 科普：Integer a = 100; 这个的意思是直接在栈上分配内存空间，而100 < 127 ，当Integer b = 100时，复用之前的数据记录，对象相等。
         *      而 a = new Integer(100)， 这个方式是直接采用堆中分配内存的方式进行，所以每次 new Integer()产生的对象是不同的。
         *
         *      至于直接将 a = 100 和 new Integer(100), 一个在栈中（局部变量表），一个在堆中，所以就不可能相同。
         */
        Integer z1 = 3;
        Integer z2 = 3;
        Integer z3 = 300;
        Integer z4 = 300;
        Integer z5 = new Integer(100);
        Integer z6 = new Integer(100);
        Integer z7 = 100;
        Integer z8 = new Integer(100);
        System.out.println(z1 == z2);
        System.out.println(z3 == z4);
        System.out.println(z5 == z6);
        System.out.println(z7 == z8);
    }

}
