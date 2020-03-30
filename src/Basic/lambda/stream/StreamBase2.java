package Basic.lambda.stream;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.*;
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
        List<String> list = s.collect(Collectors.toList());
        // Set<String> set = stream.collect(Collectors.toSet());
        // Map<String, Integer> map = stream.collect(Collectors.toMap(Function.identity(), String::length));

        // what is Function.identity() ?
        // what is String::length ?
        // what is Collectors ?

        /**
         * Function.identity() ??
         *
         *  Java 8中对接口进行了扩展，新增了 default method() 和 static method()， 而 identity() 就是 Function接口的一个静态方法
         *
         *  返回一个输出跟输入一样的Lambda表达式对象
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
         **/



         /**
         *
         * String :: length ?
         *
         *  方法引用：如果Lambda表达式的全部内容就是调用一个已有的方法，那么可以用方法引用的方式来代替Lambda表达式。
         *
         *  引用静态方法：Integer :: sum
         *  引用对象方法：list :: add
         *  引用类的方法：String :: length
         *  引用构造方法: HashMap :: new
         */


        /**
         * collector ?
         *
         * 收集器（Collector）是为Stream.collect()方法量身打造的工具接口（类）。考虑一下将一个Stream转换成一个容器（或者Map）需要做哪些工作？
         *
         * 目标容器 + 如何添加进去？ + 多个结果如何合并？
         *
         *      collect()方法定义为<R> R collect(Supplier<R> supplier, BiConsumer<R,? super T> accumulator, BiConsumer<R,R> combiner)
         */
        s = Stream.of("I", "love", "you", "too");
        List<String> collector1 = s.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);     // 方法一
        // s.collect(Collectors.toList());                                                          // 方法二
        collector1.forEach(e -> System.out.println(e));


        // 指定具体的容器类型
        s = Stream.of("I", "love", "you", "too");
        ArrayList specificList = s.collect(Collectors.toCollection(ArrayList::new));
        // HashSet specificSet = s.collect(Collectors.toCollection(HashSet::new));




        // toMap
        // 试用toMap 计算学生的总成绩
        Department d1 = new Department(1);
        Department d2 = new Department(2);
        Department d3 = new Department(3);
        Student s1 = new Student("heiku", 100, 100, 100, d1);
        Student s2 = new Student("mystic", 80, 87, 70, d2);
        Student s3 = new Student("godry", 80, 100, 100, d2);
        Student s4 = new Student("world", 98, 97, 70, d3);
        Student s5 = new Student("me", 89, 91, 70, d3);
        List<Student> studentList = new ArrayList<>();
        studentList.add(s1);
        studentList.add(s2);
        studentList.add(s3);
        studentList.add(s4);
        studentList.add(s5);

        Map<Student, Integer> studentGrade =
                studentList.stream().collect(Collectors.toMap(Function.identity(),      // 如何生成key
                                            student -> computeGrade(student)));         // 如何生成value
        studentGrade.forEach((k, v) -> System.out.println("k: " + k + " v: " + v));



        // 二分成绩值，将语文成绩值大于90的区分
        Map<Boolean, List<Student>> passingFailing =
                studentList.stream().collect(Collectors.partitioningBy(
                        stu -> stu.getChinese() >= 90
                ));
        passingFailing.forEach((k, v) -> System.out.println("k: " + k + " v: " + v));



        // 分组，根据英语成绩值分组，类似于 SQL 中的 groupBy
        Map<Integer, List<Student>> englishMap =
                studentList.stream().collect(Collectors.groupingBy(
                        Student::getEnglish
                ));
        englishMap.forEach((k, v) -> System.out.println("k: " + k + " v: " + v));




        // 简单的分组后，试试分组的扩展，将分组后的数据通过上游收集器 & 下游收集器 进行整合
        // 获取每个部门的总人数
        /*Map<Department, Integer> departCoutMap =
                studentList.stream()
                        .collect(Collectors.groupingBy(Student::getDepartment,
                                Collectors.counting()));*/

        Map<Department, List<String>> byDept = studentList.stream()
                .collect(Collectors.groupingBy(
                        Student::getDepartment,         // key  group by
                        Collectors.mapping(Student::getName,    // 下游收集器        // 将结果映射 List中
                                Collectors.toList())));             // 更下游的收集器
        byDept.forEach((k, v) -> System.out.println("k: " + k + " v: " + v));





        // collect() 拼接 String
        Stream<String> strStream = Stream.of("I", "love", "you");
        // String str1 = strStream.collect(Collectors.joining());      // Iloveyou
        // String str1 = strStream.collect(Collectors.joining(","));       // I,love,you
        String str1 = strStream.collect(Collectors.joining(",", "{", "}"));     // {I,love,you}
        System.out.println(str1);

    }

    private static Integer computeGrade(Student student){
        return student.getChinese() + student.getMath() + student.getEnglish();
    }
}


@Data
@AllArgsConstructor
class Student{

    private String name;
    private Integer chinese;
    private Integer math;
    private Integer english;

    private Department department;
}

@Data
@AllArgsConstructor
class Department{
    private Integer id;
}