package basic.random;

import java.util.Random;

/**
 * use same seed will generate same random
 *
 * @Author: Heiku
 * @Date: 2019/11/12
 */
public class RandomTest {
    public static void main(String[] args) {
        Random r1 = new Random(100);
        Random r2 = new Random(100);
        long a = 8682522807148012L;
        System.out.println(Long.MAX_VALUE);

        System.out.println(15 & 14);


/*        for (int i = 0; i < 10; i++){

            //System.out.println(r1.nextInt() + " " + r2.nextInt());
        }*/
    }
}
