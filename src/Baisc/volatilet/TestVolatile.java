package Baisc.volatilet;

import java.util.concurrent.CountDownLatch;

/**
 * @Author: Heiku
 * @Date: 2019/11/4
 */
public class TestVolatile extends  Thread{

    public volatile boolean flag = true;

    public boolean isFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    @Override
    public void run() {
        System.out.println("进入 run");
        while (isFlag()){

        }
        System.out.println("退出 run");
    }

    public static void main(String[] args) throws InterruptedException {
        TestVolatile t = new TestVolatile();
        t.start();
/*
        try {
            Thread.sleep(1000);
        }catch (InterruptedException e){
            e.printStackTrace();
        }
*/

        t.setFlag(false);
        System.out.println("main already set flag to false");
        new CountDownLatch(1).wait();
    }
}
