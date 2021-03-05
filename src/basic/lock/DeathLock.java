package basic.lock;

/**
 * 1. jps:          find out java process id
 * 2. jstack id:    explore what happened
 *
 *
 *
 * Found one Java-level deadlock:
 * =============================
 * "Thread-1":
 *   waiting to lock monitor 0x00000000179a0c58 (object 0x00000000d64b3608, a java.lang.String),
 *   which is held by "Thread-0"
 * "Thread-0":
 *   waiting to lock monitor 0x00000000179a3648 (object 0x00000000d6600468, a java.lang.String),
 *   which is held by "Thread-1"
 *
 *
 * @Author: Heiku
 * @Date: 2019/12/11
 */
public class DeathLock {
    public static void main(String[] args) {
        new Thread(() -> {
            try {
                new DeathLock().resource1();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                new DeathLock().resource2();
            }catch (Exception e){
                e.printStackTrace();
            }
        }).start();
    }

    void resource1() throws Exception{
        synchronized ("resource1"){
            System.out.println("hold resource 1");
            Thread.sleep(1000);

            resource2();
        }
    }

    void resource2() throws Exception{
        synchronized ("resource2"){
            System.out.println("hold resource 2");
            Thread.sleep(1000);

            resource1();
        }
        
    }


}
