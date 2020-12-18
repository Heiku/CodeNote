package Basic.base.number;

/**
 * @Author: Heiku
 * @Date: 2020/12/18
 */
public class NumberTest {
    public static void main(String[] args) {
        float f1 = 0.15f;
        float f2 = 0.45f / 3;   // 0.14999999

        double d1 = 24 / 7;           // 3.0
        double d2 = (double) 24 / 7;  // 3.42857

        long l1 = Integer.MAX_VALUE * 2; // 结果是溢出的－2
        long l2 = Integer.MAX_VALUE * 2L; //结果是正确的4294967294

        int abs1 = -4 % 3;      // -1, 负数取模依旧负数
        int abs2 = Math.abs(Integer.MIN_VALUE); // -2147483648;
    }
}
