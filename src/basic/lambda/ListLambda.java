package basic.lambda;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * @Author: Heiku
 * @Date: 2019/7/26
 *
 * 使用 Lambda 函数式操作 List
 */
public class ListLambda {

    public static void main(String[] args) {

        ArrayList<String> list = new ArrayList<>(Arrays.asList("I", "love", "you", "too"));

        // foreach() 结合匿名内部类
        list.forEach(new Consumer<String>() {
            @Override
            public void accept(String s) {
                if (s.length() > 3){
                    System.out.println(s);
                }
            }
        });

        // foreach() lambda
        list.forEach(str -> {
            if (str.length() > 3){
                System.out.println(str);
            }
        });


        // removeIf() 结合匿名内部类
        list = new ArrayList<>(Arrays.asList("I", "love", "you", "too"));
        list.removeIf(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.length() > 3;
            }
        });


        // removeIf() lambda 8
        list = new ArrayList<>(Arrays.asList("I", "love", "you", "too"));
        list.removeIf(str ->
            str.length() > 3
        );

        // replaceAll() 结合匿名内部类
        list = new ArrayList<>(Arrays.asList("I", "love", "you", "too"));
        list.replaceAll(new UnaryOperator<String>() {
            @Override
            public String apply(String s) {
                if (s.length() > 3){
                    return s.toUpperCase();
                }
                return s;
            }
        });

        // replaceAll() lambda
        list = new ArrayList<>(Arrays.asList("I", "love", "you", "too"));
        list.replaceAll(s -> {
            if (s.length() > 3){
                return s.toUpperCase();
            }
            return s;
        });


        // sort() 结合匿名内部类
        list = new ArrayList<>(Arrays.asList("I", "love", "you", "too"));
        Collections.sort(list, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.length() - o2.length();
            }
        });

        // sort() lambda
        list = new ArrayList<>(Arrays.asList("I", "love", "you", "too"));
        list.sort(((o1, o2) -> o2.length() - o1.length()));
        System.out.println(list);

    }
}
