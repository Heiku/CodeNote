package basic.lambda.stream;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * limit 返回指定的数量
 *
 * @Author: Heiku
 * @Date: 2020/3/20
 */
public class StreamLimitTest {
    public static void main(String[] args) {
        List<Integer> numList = IntStream.range(1, 100)
                .limit(10)
                // boxed int -> Integer
                .boxed()
                .collect(Collectors.toList());
        System.out.println(numList);
    }
}
