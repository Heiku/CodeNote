package basic.lock.synchronize;

/**
 * @Author: Heiku
 * @Date: 2019/12/11
 */
public class SynchronizedReent {

    private static Object lock = new Object();

    public static void main(String[] args) {
        synchronized (lock){
            System.out.println("first hold lock");
            synchronized (lock){
                System.out.println("second hold lock");
            }
        }
    }
}
