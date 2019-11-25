package Baisc.volatilet;

/**
 * Why use singleton in here?
 *
 * singleton = new Singleton():
 *      1.allocate memory space for object
 *      2.init object
 *      3.singleton pointer to memory space
 *
 * use volatile for stopping instruction reordering
 *
 * @Author: Heiku
 * @Date: 2019/11/25
 */
public class VolatileSingleton {

    private volatile VolatileSingleton singleton;

    public VolatileSingleton getSingletom(){
        if (singleton == null){
            synchronized (singleton){
                if (singleton == null){
                    singleton = new VolatileSingleton();
                }
            }
        }
        return singleton;
    }
}
