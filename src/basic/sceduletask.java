package basic;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;

import java.util.concurrent.TimeUnit;

/**
 * @Author: Heiku
 * @Date: 2019/11/25
 */
public class sceduletask {

    public static void main(String[] args) {
        HashedWheelTimer timer = new HashedWheelTimer();

        TimerTask task = new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                System.out.println("Timer task print");
            }
        };
        timer.newTimeout(task, 20, TimeUnit.SECONDS);
        timer.start();
    }
}
