package basic.jucutils.cyclicbarrier;

import java.util.Random;
import java.util.concurrent.CyclicBarrier;

/**
 * @Author: Heiku
 * @Date: 2019/12/13
 */
public class Athlete implements Runnable {

    private CyclicBarrier cyclicBarrier;
    private String name;

    public Athlete(CyclicBarrier cyclicBarrier, String name){
        this.cyclicBarrier = cyclicBarrier;
        this.name = name;
    }

    @Override
    public void run() {
        System.out.println(name + " ready");
        try {
            cyclicBarrier.await();

            Random random = new Random();
            double time = random.nextDouble() + 9;
            System.out.println(name + " : " + time);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
