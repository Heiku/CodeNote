package Basic.tmp;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Heiku
 * @Date: 2019/7/18
 */

class Fruit{}

class Apple extends Fruit{}
class Lemon extends Fruit{}

class Eureka extends Lemon{}

public class GenericTest {

    public static void main(String[] args) {

       /* List<? extends Fruit> list = new ArrayList<Lemon>();

        Object object = list.get(0);
        Fruit fruit = list.get(1);
        Lemon lemon = list.get(2);      // 编译错误

        list.add(new Lemon());          // 编译错误
        list.add(new Fruit());          // 编译错误*/



   /*     Fruit[] fruit = new Lemon[20];

        fruit[0] = new Lemon();
        fruit[1] = new Eureka();

        try {
            fruit[2] = new Fruit();
        }catch (Exception e){
            System.out.println(e);
        }*/


        List<? super Fruit> list = new ArrayList<>();
        list.add(new Eureka());
        list.add(new Lemon());
        list.add(new Fruit());

        /*Lemon lemon = list.get(0);      // 编译失败
        Fruit fruit = list.get(0);   */   // 编译失败

        Object obj = list.get(0);
    }
}
