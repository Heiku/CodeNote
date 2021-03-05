package basic.collections;

import java.util.TreeMap;

/**
 * @Author: Heiku
 * @Date: 2020/3/27
 */
public class TreeMapTest {
    public static void main(String[] args) {
        TreeMap<Integer, String> map = new TreeMap<>();
        map.put(3, "A");
        map.put(10, "B");

        System.out.println(map.floorKey(2));
        System.out.println(map.floorEntry(2));
    }
}
