package basic.volatilet;

import java.util.Scanner;

/**
 * @Author: Heiku
 * @Date: 2019/11/25
 */
public class VolatileStop implements Runnable {

    private static volatile boolean flag = true;

    @Override
    public void run() {
        while (flag){

        }
        System.out.println(Thread.currentThread().getName() + " done");
    }

    public static void main(String[] args) throws Exception {
        VolatileStop v = new VolatileStop();
        new Thread(v, "thread A").start();

        System.out.println("main thread running .");

        Scanner sc = new Scanner(System.in);
        while (sc.hasNext()){
            String value = sc.next();
            if (value.equals("1")){

                new Thread(() ->
                    v.stopThread()).start();

                break;
            }
        }
        System.out.println("main thread exit .");
    }


    private void stopThread(){
        flag = false;
    }
}
