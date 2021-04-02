package basic.base.singleton;

/**
 * @Author: Heiku
 * @Date: 2021/4/2
 */
public class SingletonTest2 {

    private volatile SingletonTest2 instance;

    public SingletonTest2 getSingleton() {
        if (instance == null) {
            synchronized (SingletonTest2.class) {
                if (instance == null) {
                    instance = new SingletonTest2();
                }
            }
        }
        return instance;
    }
}
