package basic.base.string;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

/**
 * jdk1.7 后，将字符串常量池放入到堆中
 * <p>
 * 参考：https://tech.meituan.com/2014/03/06/in-depth-understanding-string-intern.html
 *
 * @Author: Heiku
 * @Date: 2021/2/23
 */
public class StringInternTest {

    @Test
    public void test1() {
        // 1.先在堆中创建两个对象，一个是堆中 new 出来的 string 实例，二是字符串常量池中的 1 字符串对象
        //          s3 —> heap string("11")        string pool string("1") 无人指向
        // 2. s3.intern()，因为常量池中没有"11"，所以在常量池中生成"11"，并将 s3 的引用指向自身
        //          s3 -> heap string("11") -> string pool string("11")
        // 3. s4显式声明 "11"，会从字符串常量池中找, stringTable，发现后已有对象，是指向 s3引用对象的一个引用，所以s3和s4指向一样
        //          s3 -> heap string("11") -> string pool string("11")
        //          s3 -> string pool string("11")
        String s3 = "1" + "1";
        s3.intern();
        String s4 = "11";
        System.out.println(s3 == s4);

        // 1.先创建两个对象，堆中一个 heap string("1")，字符串常量池中一个 string pool string("1")
        //          s -> heap string("1")       string pool string("1")
        // 2. s.intern() 去字符串常量池中找，发现已经存在，返回
        //          s -> heap string("1")
        // 3. s2 显式声明 "1"，会从字符串常量池中找，stringTable，发现有，则将自身引用指向它
        //          s -> heap string("1")
        //          s2 -> string pool string("1")
        String s = "1";
        s.intern();
        String s2 = "1";
        System.out.println(s == s2);
    }

    @Test
    public void test2() {
        String s = "1";
        String s2 = "1";
        s.intern();
        System.out.println(s == s2);

        String s3 = "1" + "1";
        String s4 = "11";
        s.intern();
        System.out.println(s3 == s4);
    }

    @Test
    public void test3() {
        // 1. new String 只在堆中创建 def
        // 2. s.intern()，发现常量池中没有 def，在常量池中生成 def，然后将 s2的引用指向常量池的def
        // 3. s3常量定义优先去常量池找，发现有，那么直接返回常量池def地址
        // 所以 s2 == s3
        String s = "def";
        String s2 = s.intern();
        String s3 = "def";
        System.out.println(s2 == s3);
    }

    public static void main(String[] args) {
        ArrayList<String> stringList = new ArrayList<>();
        stringList.add("a");
        stringList.add("b");
        stringList.add("c");

        stringList.forEach(str -> {
            if (str.equals("b")) return; // only skips this iteration.

            System.out.println(str);
        });
    }
}
