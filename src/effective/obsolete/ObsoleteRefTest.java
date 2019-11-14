package effective.obsolete;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Obsolete Reference
 *
 * try to clean obj if not use, or it occurs disk paging / OutOfMemoryError
 *
 * you can use (WeakHashMap, LinkedHashMap.removeEldestEntry, java.lang.ref) or set null directly
 *
 * @Author: Heiku
 * @Date: 2019/11/14
 */
public class ObsoleteRefTest {
    public static void main(String[] args) throws Exception {

        Object obj = new Object();
        WeakReference<Object> weakReference = new WeakReference<>(obj);
        Map<Object, String> weakMap = new WeakHashMap<>();
        weakMap.put(obj, "obj");
        weakMap.put(weakReference.get(), "weak");
        System.out.println(weakMap.size());

        obj = null;

        // try to remove obsolete ref
        System.gc();
        Thread.sleep(2000);

        System.out.println(weakMap.size());
        System.out.println(weakMap.values());
    }
}
