package Baisc.lambda;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * @Author: Heiku
 * @Date: 2019/7/26
 *
 * 使用 Lambda 函数式操作 Map
 */
public class MapLambda {
    public static void main(String[] args) {

        // foreach() 结合内部匿名类
        HashMap<Integer, String> map = new HashMap<>();
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        map.forEach(new BiConsumer<Integer, String>() {
            @Override
            public void accept(Integer k, String v) {
                System.out.println(k + " = " + v);
            }
        });

        // foreach() lambda
        map.forEach((k, v) -> {
            System.out.println(k + " = " + v);
        });

        // getOrDefault(): 用于处理未找到的值
        System.out.println(map.getOrDefault(4, "no value"));

        // replaceAll() lambda
        map.replaceAll((k, v) -> v.toUpperCase());


        // computeIfAbsent()
        // 1.7
        Map<Integer, Set<String>> mapSet = new HashMap<>();
        if (mapSet.containsKey(1)){
            mapSet.get(1).add("one");
        }else {
            Set<String> set = new HashSet<>();
            set.add("one");
            mapSet.put(1, set);
        }
        // 1.8
        mapSet.computeIfAbsent(1, v -> new HashSet<>()).add("yi");



    }
}
