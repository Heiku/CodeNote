package Basic.lambda.stream;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * stream flatMap
 * <p>
 * flatMap = map + flattern, 它将映射后的流的元素全部放入到一个新的流中。
 *
 * @Author: Heiku
 * @Date: 2020/3/20
 */
public class StreamFlatMapTest {
    public static void main(String[] args) {

        String poetry = "Where, before me, are the ages that have gone?\n" +
                "And where, behind me, are the coming generations?\n" +
                "I think of heaven and earth, without limit, without end,\n" +
                "And I am all alone and my tears fall down.";

        // flatMap 用于组合多个流
        Stream<String> lines = Arrays.stream(poetry.split("\n"));
        Stream<String> words = lines.flatMap(line -> Arrays.stream(line.split(" ")));

        List<String> l = words.map(w -> {
            if (w.endsWith(",") || w.endsWith(".") || w.endsWith("?"))
                return w.substring(0, w.length() - 1).trim().toLowerCase();
            else
                return w.trim().toLowerCase();
        }).distinct().sorted().collect(Collectors.toList());

        System.out.println(l);
    }
}
