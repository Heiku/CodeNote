package basic.collections;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @Author: Heiku
 * @Date: 2019/12/5
 */
public class SortedMapTest {

    @Test
    public void hashMap(){
        Map<String, String> map = new HashMap<>();
        map.put("apple", "苹果");
        map.put("banana", "香蕉");
        map.put("lemon", "柠檬");
        map.put("watermelon", "西瓜");

        map.forEach((k, v) -> {
            System.out.println(k + " : " + v);
        } );

        /**
         * banana : 香蕉
         * apple : 苹果
         * lemon : 柠檬
         * watermelon : 西瓜
         */
    }

    @Test
    public void linkedHashMap(){
        Map<String, String> linkedMap = new LinkedHashMap<>();
        linkedMap.put("apple", "苹果");
        linkedMap.put("banana", "香蕉");
        linkedMap.put("lemon", "柠檬");
        linkedMap.put("watermelon", "西瓜");

        linkedMap.forEach((k, v) -> {
            System.out.println(k + " : " + v);
        } );

        /**
         * apple : 苹果
         * banana : 香蕉
         * lemon : 柠檬
         * watermelon : 西瓜
         */
    }
}
