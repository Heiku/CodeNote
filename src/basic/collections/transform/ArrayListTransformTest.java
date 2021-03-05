package basic.collections.transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: Heiku
 * @Date: 2020/12/22
 */
public class ArrayListTransformTest {
    public static void main(String[] args) {
        List<String> strList = new ArrayList<>();

        // list -> string[]
        String[] strArr = strList.toArray(new String[0]);
        String[] strArr2 = (String[]) strList.toArray();

        // array -> list (not expand)
        List unExpandList = Arrays.asList(strArr);

        // expand list
        List expandList = new ArrayList(strArr.length);
        Collections.addAll(expandList, strArr);

        // base type arr
        int[] baseArr = new int[0];
        List baseList = Arrays.stream(baseArr).boxed().collect(Collectors.toList());
    }
}
