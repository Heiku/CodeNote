package basic.lambda.stream;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * map: 将当前的 stream 中每一个值进行 映射修改，需要配合 collect() 进行回收
 * <p>
 * a -> a1
 * b -> b1
 * c -> c1
 * <p>
 * collect(a1, b1, c1)
 *
 * @Author: Heiku
 * @Date: 2020/3/20
 */
public class StreamMapTest {
    public static void main(String[] args) {
        List<Integer> list = Stream.of('a', 'b', 'c')
                .map(Object::hashCode)
                .collect(Collectors.toList());
        System.out.println(list);
    }
}
