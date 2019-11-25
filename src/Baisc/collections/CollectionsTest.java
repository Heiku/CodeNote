package Baisc.collections;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark                                        Mode  Cnt       Score        Error  Units
 * Baisc.collections.CollectionsTest.arrayList      avgt    5   21457.992 ±  17839.253  us/op
 * Baisc.collections.CollectionsTest.arrayListSize  avgt    5   11327.836 ±   2220.602  us/op
 * Baisc.collections.CollectionsTest.linkedList     avgt    5  115709.158 ± 310820.290  us/op
 *
 *
 * @Author: Heiku
 * @Date: 2019/11/25
 */

@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
public class CollectionsTest {

    private static final int TEN_MILLION = 1000000;

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void arrayList(){
        List<String> array = new ArrayList<>();
        for (int i = 0; i < TEN_MILLION; i++) {
            array.add("123");
        }
    }


    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void arrayListSize() {
        List<String> array = new ArrayList<String>(TEN_MILLION);

        for (int i = 0; i < TEN_MILLION; i++) {
            array.add("123");
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void linkedList() {
        List<String> array = new LinkedList<>();

        for (int i = 0; i < TEN_MILLION; i++) {
            array.add("123");
        }
    }


    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(CollectionsTest.class.getSimpleName())
                .forks(1)
                .build();


        new Runner(opt).run();
    }
}
