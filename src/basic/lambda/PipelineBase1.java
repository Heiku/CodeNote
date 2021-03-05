package basic.lambda;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * @Author: Heiku
 * @Date: 2019/7/30
 *
 *
 * Stream 的操作分类： 中间操作(Intermediate operations) & 结束操作(Terminal operations)
 *
 * 中间操作：中间操作只是一种标记，只有结束操作才会触发实时计算。
 *
 * 中间操作又分为：
 *      无状态(Stateless)：无状态中间操作是指元素的处理不受前面元素的影响
 *
 *          unordered() filter() map() mapToInt() mapToLong() mapToDouble()
 *          flatMap() flatMapToInt() flatMapToLong() flatMapToDouble() peek()
 *
 *
 *      有状态(Stateful)： 有状态的中间操作必须等到所有元素处理之后才能知道最终结果
 *
 *          distinct() sorted() sorted() limit() skip()
 *
 * 结束操作又分为：
 *      短路操作(short-circuiting)：指不用处理全部元素就可以返回结果，如找到第一个满足条件的元素
 *
 *          anyMatch() allMatch() noneMatch() findFirst() findAny()
 *
 *      非短路操作：必须处理完所有的元素再进行返回
 *
 *          forEach() forEachOrdered() toArray() reduce() collect() max() min() count()
 *
 *
 */
public class PipelineBase1 {
    public static void main(String[] args) {

        IntStream.range(1, 10)
                .peek(x -> System.out.print("\nA" + x))
                .limit(3)
                .peek(x -> System.out.print("B" + x))
                .forEach(x -> System.out.print("C" + x));

        IntStream.range(1, 10)
                .peek(x -> System.out.print("\nA" + x))
                .skip(6)
                .peek(x -> System.out.print("B" + x))
                .forEach(x -> System.out.print("C" + x));



        // 求出以 A 开头的最长字符串的长度
        List<String> list = Arrays.asList("Apple", "Bug", "ABC", "Dog");
        int maxLen = list.stream()
                .filter(s -> s.startsWith("A"))
                .mapToInt(String::length)
                .max().getAsInt();
        System.out.println(maxLen);
    }
}
