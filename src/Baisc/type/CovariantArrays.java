package Baisc.type;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Heiku
 * @Date: 2019/7/14
 *
 *
 * 协变 逆变
 *
 * 逆变与协变用来描述类型转换（type transformation）后的继承关系，
 * 其定义：如果A、B表示类型，f(⋅)表示类型转换，≤表示继承关系（比如，A≤B表示A是由B派生出来的子类）
 *
 * f(⋅)是逆变（contravariant）的，当A ⊆ B时有f(B) ⊆ f(A)成立；⊆
 * f(⋅)是协变（covariant）的，当A ⊆ B时有f(A) ⊆ f(B)成立；
 * f(⋅)是不变（invariant）的，当A ⊆ B时上述两个式子均不成立，即f(A)与f(B)相互之间没有继承关系。
 */

class Fruit {}
class Apple extends Fruit {}
class Jonathan extends Apple {}
class Orange extends Fruit {}

public class CovariantArrays {
    public static void main(String[] args) {

        // 数组是协变的
        // 数组在语言中是完全定义的，因此内建了编译器和运行时的检查
        Fruit[] fruits = new Apple[10];
        fruits[0] = new Apple();
        fruits[1] = new Jonathan();

        try {
            fruits[0] = new Fruit();
        }catch (Exception er){
            System.out.println(er);
        }

        try {
            fruits[0] = new Orange();
        }catch (Exception er){
            System.out.println(er);
        }

        // 泛型是不变的
        // 与数组不同，泛型没有内建的协变类型，类型信息在编译期间被擦除了（类型擦除）
        // List<Fruit> list = new ArrayList<Apple>();

        // 编译器不知道 List<? extends Fruit>所持有的具体类型是什么，一旦执行这种类型的向上转型，就将丢失其中传递任何对象的能力
        // 类比数组 Apple[]向上转型成 Fruit[]，然而往里面添加 Fruit和Orange都是非法的，会在运行期间抛出 ArrayStoreException
        //      泛型将这种类型检查移到了编译期间，协变过程中丢掉类型信息，编译器拒绝所有不安全的操作
        // List<? extends Fruit> list = new ArrayList<Apple>();
        // list.add(new Apple());
        // list.add(new Fruit());
        // list.add(new Object());
        // list.add(new Orange());

    }


    // 那么逆变呢？
    // super关键字指出泛型的下界为 Apple， <? super T>称为超类通配符，代表一个具体类型，这个类型是Apple的超类
    // 这样添加的 Apple 和 Jonathan就是安全的
    // 通过规定了下界为 Apple，那么往List中添加 Fruit 就是不安全的了
    static void writeTo(List<? super Apple> apples){
        apples.add(new Apple());
        apples.add(new Jonathan());

        // 编译错误
        //
        // apples.add(new Fruit());
    }



    static void readFrom(List<? extends Apple> apples){
        Apple apple = apples.get(0);
        // Jonathan jonathan = apples.get(0);
        Fruit fruit = apples.get(0);
    }
}

