package Basic.unsafe;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * use unsafe can direct malloc memory, it's useful in operate large array (out of Integer.MAX_VALUE)
 *
 * and it can free of influence from gc, it is necessary to free memory.
 *
 * @Author: Heiku
 * @Date: 2019/11/26
 */
public class SuperArr {

    private final static int BYTE = 1;

    private long size;
    private long address;

    // Exception in thread "main" java.lang.SecurityException: Unsafe
    // not allow application classloader use unsafe direct
    // use reflect instead of it
    // private static final Unsafe unsafe = Unsafe.getUnsafe();


    public SuperArr(long size){
        this.size = size;

        // malloc memory
        address = getUnsafe().allocateMemory(size * BYTE);
    }

    public void set(long i, byte value){
        getUnsafe().putByte(address + i * BYTE, value);
    }

    public int get(long i){
        return getUnsafe().getByte(address + i * BYTE);
    }

    public long size(){
        return size;
    }

    public void free(){
        getUnsafe().freeMemory(address);
    }

    private Unsafe getUnsafe(){
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


    public static void main(String[] args) {
        long sum = 0L;
        long size = (long) Integer.MAX_VALUE * 2;

        SuperArr arr = new SuperArr(size);
        System.out.println("Arr Size : " + arr.size());
        for (int i = 0; i <= 100; i++){
            arr.set((long) Integer.MAX_VALUE + i, (byte)i);
            sum += arr.get((long) Integer.MAX_VALUE + i);
        }

        System.out.println("Sum of arr: " + sum);
        arr.free();
    }

}
