package basic.jvm.gc;

/**
 * @Author: Heiku
 * @Date: 2019/12/18
 */
public class FinalizerTest {
    public static FinalizerTest object;

    public void isAlive() {
        System.out.println("I'm alive");
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        System.out.println("method finalize is calling");
        object = this;
    }


    /**
     * method finalize is calling
     * I'm alive
     * I'm dead
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        object = new FinalizerTest();

        // first call, finalize() is last chance to save self
        object = null;
        System.gc();

        Thread.sleep(1000);
        if (object != null) {
            object.isAlive();
        } else {
            System.out.println("I'm dead");
        }

        // second call, finalize() called
        object = null;
        System.gc();

        Thread.sleep(1000);
        if (object != null) {
            object.isAlive();
        } else {
            System.out.println("I'm dead");
        }
    }
}
