package Basic.thread;

/**
 * @Author: Heiku
 * @Date: 2019/12/10
 */
public class ThreadInterrupt {
    final static Object object = new Object();

    public static void main(String[] args) throws Exception{
        Thread thread = new Thread(() -> {
            synchronized (object){
                while (true){
                    System.out.println("go ");
                }
            }
        });
        thread.setName("thread-A");
        thread.start();


        Thread.sleep(3000);

        thread.interrupt();
    }
}
