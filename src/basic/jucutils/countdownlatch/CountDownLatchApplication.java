package basic.jucutils.countdownlatch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @Author: Heiku
 * @Date: 2019/12/13
 */
public class CountDownLatchApplication {
    private CountDownLatch latch;

    public void startUp() throws Exception {
        latch = new CountDownLatch(2);

        List<Service> services = new ArrayList<>();
        services.add(new HealthCheckService(latch));
        services.add(new DatabaseCheckService(latch));

        Executor executor = Executors.newFixedThreadPool(services.size());
        services.forEach(executor::execute);

        new Thread(() -> {
            try {
                latch.await();
            } catch (Exception e) {
                e.printStackTrace();
            }
            for (int i = 0; i < 3; i++) {
                System.out.println("custom thread free");
            }
        }).start();
        latch.await();
        System.out.println("all service is start up");
    }

    public static void main(String[] args) throws Exception {
        new CountDownLatchApplication().startUp();
    }
}
