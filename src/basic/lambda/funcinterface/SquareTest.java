package basic.lambda.funcinterface;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * @Author: Heiku
 * @Date: 2020/1/2
 */
public class SquareTest {
    public static void main(String[] args) {
        int a = 5;
        Square s = (x) -> x * x;
        int score = s.cal(a);
        System.out.println(score);


        // generic ask the type of param
        List<String> names = Arrays.asList("Greek", "GreekQuiz", "g1", "QA", "Geek2");
        Predicate<String> p = (str) -> str.startsWith("G");
        names.forEach(s1 -> {
            if (p.test(s1)) {
                System.out.println(s1);
            }
        });

    }
}

@FunctionalInterface
interface Square {
    int cal(int x);
}


@FunctionalInterface
interface GFilter {
    boolean test(String s);
}
