package Basic.jucutils.countdownlatch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @Author: Heiku
 * @Date: 2019/12/13
 */
public class DatabaseCheckService extends Service {

    public DatabaseCheckService(CountDownLatch latch){
        super(latch);
    }

    @Override
    public void execute() {
        try {
            TimeUnit.SECONDS.sleep(5);
            System.out.println("DatabaseCheckService start up done");
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }
}
