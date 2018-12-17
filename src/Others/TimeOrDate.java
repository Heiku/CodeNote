package Others;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JDK8 中建议使用 Instant 代替 Date,  LocalDateTime 代替 Calendar, DateTimeFormatter 代替 SimpleDateFormat
 *
 * 还有，SimpleDateFormat 是 线程不安全的，可以加锁，或者采用ThreadLocal
 */
public class TimeOrDate {

    // MM 大写是为了区分m， 而HH 大写是为了区分 12小时 | 24小时
    static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {

        Instant instant = Instant.now();
        System.out.println(instant);

        LocalDateTime dateTime = LocalDateTime.now();
        System.out.println(dateTime);


        /*for (int i = 0; i < 10; i++){
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        System.out.println(sdf.parse("2018-12-12 11:22:22"));
                    }catch (ParseException e){
                        e.printStackTrace();
                    }
                }
            });

            thread.start();
        }*/
    }
}
