package basic.lambda;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Optional;

/**
 * @Author: Heiku
 * @Date: 2020/3/21
 */
public class OptionalTest {
    public static void main(String[] args) {
        List<String> a = null;
        a = Optional.ofNullable(a).orElse(Lists.newArrayList());

        a.forEach(System.out::println);
    }
}
