package Baisc.type.generic;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Heiku
 * @Date: 2019/7/17
 */

class Fruit{}       // base

class Apple extends Fruit{}

class Lemon extends Fruit{}
class Eureka extends Lemon{}


public class GenericBase {
    public static void main(String[] args) {

        /*Fruit[] fruit = new Lemon[20];

        fruit[0] = new Lemon();
        fruit[1] = new Eureka();

        try {
            fruit[2] = new Fruit();
        }catch (Exception e){
            System.out.println(e);
        }*/



    }
}
