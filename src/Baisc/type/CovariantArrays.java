package Baisc.type;

/**
 * @Author: Heiku
 * @Date: 2019/7/14
 */

class Fruit {}
class Apple extends Fruit {}
class Jonathan extends Apple {}
class Orange extends Fruit {}

public class CovariantArrays {
    public static void main(String[] args) {


        // 数组是协变的
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
    }
}

