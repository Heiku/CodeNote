package effective.autoboxing;

/**
 * try to avoid the autoBoxing in cal
 *
 * @Author: Heiku
 * @Date: 2019/11/14
 */
public class AutoBoxingTest {

    public static void main(String[] args) {
        sum();      // 6140
        sum2();     // 616
    }

    private static long sum(){
        long start = System.currentTimeMillis();
        Long sum = 0L;
        for (long i = 0; i < Integer.MAX_VALUE; i++){
            sum += i;
        }
        System.out.println(System.currentTimeMillis() - start);
        return sum;
    }

    private static long sum2(){
        long start = System.currentTimeMillis();
        long sum = 0L;
        for (long i = 0; i < Integer.MAX_VALUE; i++){
            sum += i;
        }
        System.out.println(System.currentTimeMillis() - start);
        return sum;
    }

}
