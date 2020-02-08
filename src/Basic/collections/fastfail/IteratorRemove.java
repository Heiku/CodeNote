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

        // at begin, the list expectModCount = (add() remove()... operation count) = 2
        // during the iter, when we remove element(list.remove()),
        // the modCount++, but modCount != expectModCount, throw ConcurrentModifiedException

        // iter.remove() will make expectModCount = modCount

        // hasNext() -> (cursor != size)
        // Next() -> return elementData[cursor++]
        while (iter.hasNext()) {
            System.out.println("loopTimes: " + loopTimes++);

            String str = iter.next();

            // when iter loop once, cursor == size = 1, break the loop, so just remove one element, list size = 1
            //list.remove(str);

            // total loop three times,
            // in second times, the element had removed, modCount++, (expectModCount = 2)
            // in third times, hasNext(cursor = 2, size = 1, != , so continue)
            // next() will check modCount == expectModCount, but not , so will cause ConcurrentModifiedException
            if (str.equals("World")) {
                list.remove(str);
                // safe remove
                // iter.remove();
            }
        }
    }
}
