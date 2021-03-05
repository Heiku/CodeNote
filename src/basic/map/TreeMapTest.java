package basic.map;

import java.util.TreeMap;

/**
 * @Author: Heiku
 * @Date: 2019/11/22
 */
public class TreeMapTest {
    public static void main(String[] args) {

        TreeMap<Integer, String> map = new TreeMap<>();
        map.put(1, "a");
        map.put(3, "b");
        map.put(5, "c");
        map.put(7, "d");
        map.put(9, "e");

        System.out.println(map.ceilingEntry(0).getValue());
        System.out.println(map.ceilingEntry(7).getValue());
        System.out.println(map.ceilingEntry(8).getValue());
    }
}
