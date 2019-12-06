package Basic.collections;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * https://cloud.tencent.com/developer/article/1343130
 *
 * @Author: Heiku
 * @Date: 2019/12/5
 */
public class HashMapInfiniteLoop {
    public static void main(String[] args) {
        final HashMap<String, String> map = new HashMap<String, String>();
        for (int i = 0; i < 1000; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    map.put(UUID.randomUUID().toString(), "");
                }
            }).start();
        }
    }
}
