package Basic.lambda;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * @Author: Heiku
 * @Date: 2019/7/26
 *
 * BaseStream -> (IntStream, LongStream, DoubleStream, Stream, AbstractPipeline)
 *
 * 特点：
 *      无存储：stream不是一种数据结构，它只是某种数据源的一个视图，数据源可以是一个数组，Java容器或I/O channel等。
 *      为函数式编程而生：对stream的任何修改都不会修改背后的数据源，比如对stream执行过滤操作并不会删除被过滤的元素，而是会产生一个不包含被过滤元素的新stream。
 *      惰式执行：stream上的操作并不会立即执行，只有等到用户真正需要结果的时候才会执行。
 *      可消费性：stream只能被“消费”一次，一旦遍历过就会失效，就像容器的迭代器那样，想要再次遍历必须重新生成。
 *
 * stream的操作：
 *      中间操作 ， 结束操作
 *
 *      中间操作总是会惰式执行，调用中间操作只会生成一个标记了该操作的新stream，仅此而已。
 *      结束操作会触发实际计算，计算发生时会把所有中间操作积攒的操作以pipeline的方式执行，这样可以减少迭代次数。计算完成之后stream就会失效。
 *
 *      中间操作：concat() distinct() filter() flatMap() limit() map() peek()
 *              skip() sorted() parallel() sequential() unordered()
 *
 *      结束操作：llMatch() anyMatch() collect() count() findAny() findFirst()
 *              forEach() forEachOrdered() max() min() noneMatch() reduce() toArray()
 */
public class StreamBase {
    public static void main(String[] args) {

        // Stream() foreach() 迭代
        System.out.println("foreach() \n");
        Stream<String> stream = Stream.of("I", "love", "you", "too");
        stream.forEach(str -> {
            //System.out.print(str + ' ');
        });


        // filter() 函数原型为Stream<T> filter(Predicate<? super T> predicate)，作用是返回一个只包含满足predicate条件元素的Stream。
        System.out.println("filter() \n");
        stream= Stream.of("I", "love", "you", "too");
        stream.filter(s -> s.length() == 3)
                .forEach(s -> System.out.print(s + " "));


        // distinct()  函数原型为Stream<T> distinct()，作用是返回一个去除重复元素之后的Stream
        System.out.println("distinct() \n");
        stream= Stream.of("I", "love", "you", "too", "too");
        stream.distinct()
                .forEach(s -> System.out.print(s + " "));


        // sorted()
        System.out.println("sorted() \n");
        stream= Stream.of("I", "love", "you", "too");
        stream.sorted(((o1, o2) -> o2.length() - o1.length()))
                .forEach(s -> System.out.print(s + " "));


        // map() 函数原型为<R> Stream<R> map(Function<? super T,? extends R> mapper)，作用是返回一个对当前所有元素执行执行mapper之后的结果组成的Stream
        System.out.println("map() \n");
        stream= Stream.of("I", "love", "you", "too");
        stream.map(s -> s.toUpperCase())
                .forEach(s -> System.out.print(s + " "));


        // flapMap() 将stream中的集合list 拆分为多个 int个体
        Stream<List<Integer>> s = Stream.of(Arrays.asList(1,2), Arrays.asList(3, 4, 5));
        s.flatMap(list -> list.stream())
                .forEach(i -> System.out.print(i + " "));
    }
}
