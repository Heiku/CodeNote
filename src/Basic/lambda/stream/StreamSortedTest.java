package Basic.lambda.stream;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * sorted()
 *
 * @Author: Heiku
 * @Date: 2020/3/21
 */
public class StreamSortedTest {
    public static void main(String[] args) {
        String[] arr = new String[]{"b_123", "c+342", "b#632", "d_123"};
        List<String> list = Arrays.stream(arr)
                .sorted((s1, s2) -> {
                    if (s1.charAt(0) == s2.charAt(0))
                        return s1.substring(2).compareTo(s2.substring(2));
                    else
                        return s1.charAt(0) - s2.charAt(0);
                })
                .collect(Collectors.toList());
        System.out.println(list);
    }
}
