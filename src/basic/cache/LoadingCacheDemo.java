package basic.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * LoadingCache Demo
 *
 * @author heiku
 * @date 2022/5/3
 **/
public class LoadingCacheDemo {

    private LoadingCache<String, String> loadingCache = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .refreshAfterWrite(1, TimeUnit.HOURS)
            .build(new CacheLoader<String, String>() {
                @Override
                public String load(String s) throws Exception {
                    return executeLoad(s);
                }

                @Override
                public ListenableFuture<String> reload(String key, String oldValue) throws Exception {
                    return executeReload(key, oldValue);
                }
            });


    public static void main(String[] args) throws InterruptedException, ExecutionException {
        LoadingCacheDemo demo = new LoadingCacheDemo();
        String key = "data";
        while (true) {
            String data = demo.loadingCache.get(key);
            System.out.println(String.format("get data from cache, data:%s", data));
            Thread.sleep(1000L);
        }
    }


    private String executeLoad(String key) {
        System.out.println(String.format("execute loading, fetch data from src, key = %s", key));
        return null;
    }


    private ListenableFuture<String> executeReload(String key, String oldValue) {
        System.out.println(String.format("execute loading, fetch data from src, key = %s", key));
        return Futures.immediateFuture(oldValue);
    }
}
