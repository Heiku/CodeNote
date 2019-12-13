package Basic.jucutils.countdownlatch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @Author: Heiku
 * @Date: 2019/12/13
 */
public class HealthCheckService extends Service {

    public HealthCheckService(CountDownLatch latch){
        super(latch);
    }

    @Override
    public void execute() {
        try {
            TimeUnit.SECONDS.sleep(2);
            System.out.println("HealthCheckService start up done");
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }
}
