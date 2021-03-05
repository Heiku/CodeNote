package basic.unsafe;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;

/**
 * use unsafe.objectFieldOffset get object size.(compute object field set)
 *
 * @Author: Heiku
 * @Date: 2019/11/27
 */
public class SizeOfObject {

    public static long sizeOf(Object obj){
        Unsafe unsafe = UnsafeI.getUnsafe();

        HashSet<Field> fields = new HashSet<>();
        Class clazz = obj.getClass();

        while (clazz != Object.class){
            for (Field f : clazz.getDeclaredFields()) {
                // skip static field
                // static filed belong to Class
                if ((f.getModifiers() & Modifier.STATIC) == 0){
                    fields.add(f);
                }
            }
            // up root obj (except Object)
            clazz = clazz.getSuperclass();
        }

        // get offset
        long maxSize = 0;
        for (Field f : fields) {
            long offset = unsafe.objectFieldOffset(f);
            if (offset > maxSize){
                maxSize = offset;
            }
        }

        // padding
        return ((maxSize / 8) + 1) * 8;
    }


    public static void main(String[] args) {
        SizeOfObject obj = new SizeOfObject();
        System.out.println(SizeOfObject.sizeOf(obj));
    }
}
