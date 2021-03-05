package basic.optional;

import java.util.Optional;

/**
 * @Author: Heiku
 * @Date: 2019/11/25
 */
public class OptionalTest {
    public static void main(String[] args) {
        Object obj = null;
        System.out.println(Optional.ofNullable(obj).orElse(null));
        System.out.println(Integer.MAX_VALUE);
    }
}
