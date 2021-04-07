package basic.volatilet;

import java.util.concurrent.TimeUnit;

/**
 * @Author: Heiku
 * @Date: 2021/4/7
 */
public class VolatileLoop {

    private static boolean flag = false;
    private static int i = 0;

    public static void main(String[] args) {
        new Thread(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
                flag = true;
                System.out.println("flag 被修改成 true");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        while (!flag) {
            i++;
        }
        System.out.println("程序结束,i=" + i);
    }
}
