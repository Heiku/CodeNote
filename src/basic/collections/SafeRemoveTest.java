package basic.collections;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * fast-fail && safe-fail
 *
 * @Author: Heiku
 * @Date: 2019/11/29
 */
public class SafeRemoveTest {

    @Test
    public void removeList(){
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            list.add(i);
        }

        // java.util.ConcurrentModificationException
        /*list.forEach(i -> {
            if (i % 2 == 0){
                list.remove(i);
            }
        });*/

        // may occur ConcurrentModificationException in multi thread run
        Iterator<Integer> iterator = list.iterator();
        while (iterator.hasNext()){
            if (iterator.next() % 2 == 0){
                iterator.remove();
            }
        }
        System.out.println(list);
    }
}
