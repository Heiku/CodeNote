package basic.lambda.stream;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * peek 使用 Consumer 消费流中的元素，但是返回的流还是包含原来流中的元素 (类似于存了一份快照)
 * <p>
 * （对比两次 peek）
 *
 * @Author: Heiku
 * @Date: 2020/3/20
 */
public class StreamPeekTest {
    public static void main(String[] args) {
        Stream.of("one", "two", "three", "four", "five")
                .filter(e -> e.length() > 3)
                .peek(e -> System.out.println("Filtered value: " + e))
                .map(String::toUpperCase)
                .peek(e -> System.out.println("Mapped value: " + e))
                .collect(Collectors.toList());
    }
}
