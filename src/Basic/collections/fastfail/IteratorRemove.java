package Basic.collections.fastfail;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @Author: Heiku
 * @Date: 2020/1/16
 */
public class IteratorRemove {
    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        list.add("Hello");
        list.add("World");

        Iterator<String> iter = list.iterator();
        int loopTimes = 1;

        // at begin, the list expectModCount = (add() num) 2,
        // 
        while (iter.hasNext()) {
            System.out.println("loopTimes: " + loopTimes++);

            String str = iter.next();
            if (str.equals("World")) {
                list.remove(str);
            }
        }
    }
}
