package basic.base.singleton;

/**
 * @Author: Heiku
 * @Date: 2021/4/2
 */
public class SingletonTest1 {

    public SingletonTest1 getSingleton() {
        return SingletonHolder.instance;
    }

    private static class SingletonHolder {
        private static final SingletonTest1 instance = new SingletonTest1();
    }
}
