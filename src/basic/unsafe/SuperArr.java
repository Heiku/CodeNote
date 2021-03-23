package basic.unsafe;

/**
 * use unsafe can direct malloc memory, it's useful in operate large array (out of Integer.MAX_VALUE)
 * <p>
 * and it can free of influence from gc, it is necessary to free memory.
 *
 * @Author: Heiku
 * @Date: 2019/11/26
 */
public class SuperArr {
/*

    private final static int BYTE = 1;

    private long size;
    private long address;

    // Exception in thread "main" java.lang.SecurityException: Unsafe
    // not allow application classloader use unsafe direct
    // use reflect instead of it
    // private static final Unsafe unsafe = Unsafe.getUnsafe();


    public SuperArr(long size) {
        this.size = size;

        // malloc memory
        address = UnsafeI.getUnsafe().allocateMemory(size * BYTE);
    }

    public void set(long i, byte value) {
        UnsafeI.getUnsafe().putByte(address + i * BYTE, value);
    }

    public int get(long i) {
        return UnsafeI.getUnsafe().getByte(address + i * BYTE);
    }

    public long size() {
        return size;
    }


    // free of no-heap memory
    public void free() {
        UnsafeI.getUnsafe().freeMemory(address);
    }


    public static void main(String[] args) {
        long sum = 0L;
        long size = (long) Integer.MAX_VALUE * 2;

        SuperArr arr = new SuperArr(size);
        System.out.println("Arr Size : " + arr.size());
        for (int i = 0; i <= 100; i++) {
            arr.set((long) Integer.MAX_VALUE + i, (byte) i);
            sum += arr.get((long) Integer.MAX_VALUE + i);
        }

        System.out.println("Sum of arr: " + sum);
        arr.free();
    }
*/

}
