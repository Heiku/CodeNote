package Baisc.lambda;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Author: Heiku
 * @Date: 2019/7/26
 *
 * 规约操作（reduction operation）又被称作折叠操作（fold），是通过某个连接动作将所有元素汇总成一个汇总结果的过程。
 *      元素求和、求最大值或最小值、求出元素总个数、将所有元素转换成一个列表或集合，都属于规约操作。
 *
 *      reduce() collect() // sum() max() min() count()
 *
 *
 *      Optional<T> reduce(BinaryOperator<T> accumulator)
 *      T reduce(T identity, BinaryOperator<T> accumulator)
 *      <U> U reduce(U identity, BiFunction<U,? super T,U> accumulator, BinaryOperator<U> combiner)
 *
 *
 *
 *
 *
 */
public class StreamBase2 {
    public static void main(String[] args) {

        // stream() reduce() 获取数组中最长的值
        Stream<String> stream = Stream.of("I", "love", "you", "too");
        Optional<String> longest = stream.reduce((s1, s2) -> s1.length() >= s2.length() ? s1 : s2);
        // Optional<String> longest2 = stream.max((s1, s2) -> s1.length() - s2.length());
        System.out.println("数组中最长的值为：" + longest.get());


        // reduce() 求出一组单词的长度之和
        stream = Stream.of("I", "love", "you", "too");
        Integer lengthSum = stream.reduce(0,   // 初始值
                (sum, str) -> sum + str.length(),       // 累加器
                (a, b) -> a + b     // 部分和拼接器，并行执行使用
        );
        System.out.println("数组单词的长度之和为：" + lengthSum);





        // collect()

        // 将Stream 转换成容器或Map
        Stream<String> s = Stream.of("I", "love", "you", "too");
        List<String> list = stream.collect(Collectors.toList());
        // Set<String> set = stream.collect(Collectors.toSet());
        // Map<String, Integer> map = stream.collect(Collectors.toMap(Function.identity(), String::length));

        // what is Function.identity() ?
        // what is String::length() ?
        // what is Collectors ?

        /**
         * Function.identity() ??
         *
         * Java 8中对接口进行了扩展，新增了 default method() 和 static method()， 而 identity() 就是 Function接口的一个静态方法
         *
         *      /**
         *      * Returns a function that always returns its input argument.
         *      *
         *      * @param <T> the type of the input and output objects to the function
         *      * @return a function that always returns its input argument
         *
         *      static <T > Function < T, T > identity() {
         *          return t -> t;
         *      }
         */
    }
}
