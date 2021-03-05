package basic.thread;

/**
 * @Author: Heiku
 * @Date: 2019/12/11
 */
public class ThreadInterrupt {

    // thread.interrupt() didn't work , always keep print
    public void directInterrupt() throws Exception{
        Thread t = new Thread(() -> {
            for (;;){
                System.out.println("keep print");
            }
        });
        t.start();

        // thread.interrupt() just set a sign in thread, didn't stop
        t.interrupt();
    }


    public void checkSignInterrupt() throws Exception {
        Thread r = new Thread(() -> {
            for (;;) {
                if (Thread.currentThread().isInterrupted()){
                    System.out.println("had interrupt");
                    break;
                }
                System.out.println("keep print");
            }
        });
        r.start();
        Thread.sleep(1000);
        r.interrupt();
    }

    public static void main(String[] args) throws Exception {
        //new ThreadInterrupt().directInterrupt();
        new ThreadInterrupt().checkSignInterrupt();
    }
}
