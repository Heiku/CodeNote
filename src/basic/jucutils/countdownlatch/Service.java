package basic.jucutils.countdownlatch;

import java.util.concurrent.CountDownLatch;

/**
 * @Author: Heiku
 * @Date: 2019/12/13
 */
public class Service implements Runnable {

    private CountDownLatch latch;

    public Service(CountDownLatch latch){
        this.latch = latch;
    }

    @Override
    public void run() {
        try {
            execute();
        }finally {
            if (latch != null){
                latch.countDown();
            }
        }
    }

    public void execute() {
    }
}
