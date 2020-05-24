package redis.delayqueue;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import redis.clients.jedis.Jedis;

import java.lang.reflect.Type;
import java.util.Set;
import java.util.UUID;

/**
 * zrangebyscore
 * zrem
 *
 * @author Heiku
 * @date 2020/5/24
 **/
public class RedisDelayQueue<T> {

    static class TaskItem<T> {
        public String id;
        public T msg;
    }

    private Type TaskType = new TypeReference<TaskItem<T>>(){}.getType();

    private Jedis jedis;
    private String queueKey;

    public RedisDelayQueue(Jedis jedis, String queueKey){
        this.jedis = jedis;
        this.queueKey = queueKey;
    }

    public void delay(T msg) {
        TaskItem<T> task = new TaskItem<>();
        task.id = UUID.randomUUID().toString();
        task.msg = msg;
        String s = JSON.toJSONString(task);
        jedis.zadd(queueKey, System.currentTimeMillis() + 5000, s); // 塞入延时队列，5s 后重试
    }

    public void loop() {
        while (!Thread.interrupted()){
            // 只取一条 [0, currentTime]
            Set<String> values = jedis.zrangeByScore(queueKey, 0, System.currentTimeMillis(), 0, 1);
            if (values.isEmpty()){
                try {
                    // 休息重试，避免 redis cpu 占用
                    Thread.sleep(500);
                }catch (InterruptedException e){
                    break;
                }
                continue;
            }
            String s = values.iterator().next();
            // 取到了就移除吧
            if (jedis.zrem(queueKey, s) > 0) {
                TaskItem<T> task = JSON.parseObject(s, TaskType);
                this.handleMsg(task.msg);
            }
        }
    }

    public void handleMsg(T msg) {
        System.out.println(msg);
    }

    public static void main(String[] args) {
        Jedis jedis = new Jedis();
        RedisDelayQueue<String> queue = new RedisDelayQueue<>(jedis, "q-demo");
        Thread producer = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                queue.delay("codehole" + i);
            }
        });
        Thread consumer = new Thread(() -> {
            queue.loop();
        });

        producer.start();
        consumer.start();
        try {
            producer.join();
            Thread.sleep(6000);
            consumer.interrupt();
            consumer.join();
        }catch (InterruptedException e) {

        }
    }
}
