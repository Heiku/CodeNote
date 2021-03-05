package basic.base.string;

/**
 * @Author: Heiku
 * @Date: 2019/12/19
 */
public class StringAppendTest {
    public static void main(String[] args) {
        for (int i = 0; i < 5; i++) {
            run1();
            run2();
        }
    }

    public static void run1() {
        long start = System.currentTimeMillis();
        String result = "";
        for (int i = 0; i < 10000; i++) {
            // "+" -> new StringBuilder & init StringBuilder & append
            result += i;
        }
        System.out.println(System.currentTimeMillis() - start);
    }

    public static void run2() {
        long start = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append(i);
        }
        System.out.println(System.currentTimeMillis() - start);
    }
}
