package cache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @Author: Heiku
 * @Date: 2020/2/10
 */
public class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int CACHE_SIZE;

    public LRUCache(int cacheSize){
        super((int) Math.ceil(cacheSize / 0.75) + 1, 0.75f, true);
        CACHE_SIZE = cacheSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() >= CACHE_SIZE;
    }
}
