package Basic.unsafe;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * @Author: Heiku
 * @Date: 2019/11/27
 */
public class UnsafeI {

    public static Unsafe getUnsafe(){
        Field f = null;         // reflect
        Unsafe unsafe = null;

        try {
            f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);

            // get unsafe
            unsafe = (Unsafe) f.get(null);
        }catch (Exception e){
            e.printStackTrace();
        }
        return unsafe;
    }
}
