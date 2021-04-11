package basic.base.singleton;

/**
 * @Author: Heiku
 * @Date: 2021/4/2
 */
public class SingletonTest1 {

    public static SingletonTest1 getInstance() {
        return SingletonHolder.instance;
    }

    static class SingletonHolder {
        private static SingletonTest1 instance = new SingletonTest1();
    }
}
