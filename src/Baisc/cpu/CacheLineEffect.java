package Baisc.cpu;

/**
 * CPU Cache
 *
 * 伪共享：数据同时缓存在同一数据行上，当这个数据行上的某一个数据被修改，那么整个数据行的数据将会失效，将重新到内存中读取
 *
 *
 * @Author: Heiku
 * @Date: 2019/10/21
 */
public class CacheLineEffect {

    static long[][] arr;

    public static void main(String[] args) {
        arr = new long[1024 * 1024][];
        for (int i = 0; i < 1024 * 1024; i++){
            arr[i] = new long[8];
            for (int j = 0; j < 8; j++){
                arr[i][j] = 0L;
            }
        }

        long sum = 0L;
        long marked = System.currentTimeMillis();
        for (int i = 0; i < 1024 * 1024; i += 1){
            for (int j = 0; j < 8; j++){
                sum = arr[i][j];
            }
        }
        System.out.println("Loop times: " + (System.currentTimeMillis() - marked) + "ms");

        marked = System.currentTimeMillis();
        for (int i = 0; i < 8; i += 1){
            for (int j = 0; j < 1024 * 1024; j++){
                sum = arr[j][i];
            }
        }
        System.out.println("Loop times: " + (System.currentTimeMillis() - marked) + "ms");

        // Loop times: 21ms
        // Loop times: 59ms
    }
}
