package basic.collections.fin;

import java.util.ArrayList;
import java.util.List;

public class FinalListTest {
    static final List<Integer> list = new ArrayList<>();

    public static void main(String[] args) {
        list.add(1);
        System.out.println(list);
    }
}
