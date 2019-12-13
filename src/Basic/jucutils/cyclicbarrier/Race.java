package Basic.jucutils.cyclicbarrier;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @Author: Heiku
 * @Date: 2019/12/13
 */
public class Race {
    private CyclicBarrier cyclicBarrier = new CyclicBarrier(6);

    public void start(){
        List<Athlete> athletes = new ArrayList<>();
        athletes.add(new Athlete(cyclicBarrier, "博尔特"));
        athletes.add(new Athlete(cyclicBarrier, "鲍威尔"));
        athletes.add(new Athlete(cyclicBarrier, "盖伊"));
        athletes.add(new Athlete(cyclicBarrier, "布雷克"));
        athletes.add(new Athlete(cyclicBarrier, "加特林"));
        athletes.add(new Athlete(cyclicBarrier, "苏炳添"));

        Executor executor = Executors.newFixedThreadPool(athletes.size());
        athletes.forEach(athlete -> executor.execute(athlete));
    }

    public static void main(String[] args) {
        new Race().start();
    }
}
