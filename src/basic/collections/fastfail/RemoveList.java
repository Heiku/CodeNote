package basic.collections.fastfail;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Heiku
 * @Date: 2020/1/16
 */
public class RemoveList {
    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        list.add("Hello");
        list.add("World");

        // Exception in thread "main" java.util.ConcurrentModificationException
        //list.forEach(e -> list.remove(e));

        // two scenes
        for (String s : list) {
            // Exception in thread "main" java.util.ConcurrentModificationException
            // at java.util.ArrayList$Itr.checkForComodification(ArrayList.java:909)
            // at java.util.ArrayList$Itr.next(ArrayList.java:859)
            // for range in Java is a syntactic sugar, for range -> Iterator
            if (s.equals("World")) {
                list.remove(s);
            }

            list.remove(s);

            // nothing happen
            // same with list.remove(s). when loop once, delete first element, this time cursor = size, so break iterator
            // hasNext() ask cursor != size, but current cursor = size, so break iterator, skip next() check modCount == expectModCount
            if (s.equals("Hello")) {
                list.remove(s);
            }

            // hasNext()
            // next()
        }
        System.out.println(list.size());
    }
}
