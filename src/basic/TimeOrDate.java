package basic;


import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * JDK8 中建议使用 Instant 代替 Date,  LocalDateTime 代替 Calendar, DateTimeFormatter 代替 SimpleDateFormat
 *
 * 还有，SimpleDateFormat 是 线程不安全的，可以加锁，或者采用ThreadLocal
 */
public class TimeOrDate {

    // MM 大写是为了区分m， 而HH 大写是为了区分 12小时 | 24小时
    static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");




    /**
     * 使用ThreadFactoryBuilder定义一个线程池
     */
    private static ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
            .setNameFormat("demo-pool-%d").build();

    private static ExecutorService pool = new ThreadPoolExecutor(5, 200,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(1024), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());

    /**
     * 定义一个CountDownLatch，保证所有子线程执行完之后主线程再执行
     */
    private static CountDownLatch countDownLatch = new CountDownLatch(100);



    /**
     * 使用ThreadLocal定义一个全局的SimpleDateFormat
     */
    private static ThreadLocal<SimpleDateFormat> simpleDateFormatThreadLocal = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }
    };

    public static void main(String[] args) throws Exception{

        /**
         * simpleDateFormat 用法
         */
        Date data = new Date();
        String s = sdf.format(data);
        System.out.println(s);


        /**
         * 推荐使用
         */
        Instant instant = Instant.now();
        System.out.println(instant);

        LocalDateTime dateTime = LocalDateTime.now();
        System.out.println(dateTime);


        /**
         * 为什么SimpleDateFormat 线程不安全
         *
         * // Called from Format after creating a FieldDelegate
         *     private StringBuffer format(Date date, StringBuffer toAppendTo,
         *                                 FieldDelegate delegate) {
         *
         *         // Convert input date to time field list
         *         calendar.setTime(date);
         *
         *         }
         *
         * 因为定义的 simpleDateFormat 是用 static修饰，即共享变量，所以多线程可自由访问sdf 中的 calendar 变量，
         * 照成多线程在 format/parse 进行时间转换的时候，多线程在 calendar.setTime() / calendar.getTime() 照成时间不一致的情况
         *
         * 解决办法：
         *      （1）采用局部变量的方式，但会频繁地创建sdf对象
         *      （2) 加锁： synchronized(sdf)
         *      （3) ThreadLocal: 确保每一个线程能得到单独的SimpleDateFormat对象
         *      （4）使用DateTimeFormatter : 线程安全
         *
         */
        //定义一个线程安全的HashSet
        Set<String> dates = Collections.synchronizedSet(new HashSet<String>());
        for (int i = 0; i < 100; i++) {
            //获取当前时间
            Calendar calendar = Calendar.getInstance();
            int finalI = i;
            pool.execute(() -> {
                //时间增加
                calendar.add(Calendar.DATE, finalI);
                //通过simpleDateFormat把时间转换成字符串
                String dateString = sdf.format(calendar.getTime());
                //把字符串放入Set中
                dates.add(dateString);
                //countDown
                countDownLatch.countDown();
            });
        }
        //阻塞，直到countDown数量为0
        countDownLatch.await();
        //输出去重后的时间个数
        System.out.println(dates.size());
    }
}
