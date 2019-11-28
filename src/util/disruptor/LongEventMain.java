package util.disruptor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.sun.corba.se.spi.orbutil.threadpool.Work;

import java.util.concurrent.*;

/**
 * @Author: Heiku
 * @Date: 2019/11/27
 */
public class LongEventMain {
    public static void main(String[] args) throws Exception {
        BlockingDeque<Runnable> queue = new LinkedBlockingDeque<>();

        ThreadFactory consumeThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("consumer-%d")
                .setDaemon(true)
                .build();

        ThreadPoolExecutor consumeExecutor = new ThreadPoolExecutor(10, 10, 1,
                TimeUnit.MILLISECONDS, queue, consumeThreadFactory);

        ThreadFactory productThreadFactoey = new ThreadFactoryBuilder()
                .setNameFormat("producer-%d")
                .setDaemon(true)
                .build();

        ThreadPoolExecutor producerExecutor = new ThreadPoolExecutor(2, 2, 1,
                TimeUnit.MILLISECONDS, queue, productThreadFactoey);



        // create factory of LongEvent
        LongEventFactory eventFactory = new LongEventFactory();

        // Specify the size of ring buffer, must be power of 2
        int bufferSize = 8;

        for (int i = 0; i < 1; i++){
            // construct the Disruptor
            Disruptor<LongEvent> disruptor = new Disruptor<>(eventFactory, bufferSize, consumeExecutor, ProducerType.SINGLE,
                    new YieldingWaitStrategy());

            // connect the handler
            disruptor.handleEventsWith(new LongEventHandler());

            // sta rt
            disruptor.start();

            // Get the ring buffer from the Disruptor to be used for publishing
            RingBuffer<LongEvent> ringBuffer = disruptor.getRingBuffer();

            LongEventProducer producer = new LongEventProducer(ringBuffer);
            for (int j = 0; j < 100; j++){
                producerExecutor.execute(new Work(producer, j));
            }
        }

        producerExecutor.shutdown();
        while (!producerExecutor.awaitTermination(1, TimeUnit.SECONDS)){
            System.out.println("consumer executor still working ");
        }
        System.out.println("main exit");
    }


    private static class Work implements Runnable{
        private LongEventProducer producer;
        private long v;

        public Work(LongEventProducer producer, long v){
            this.producer = producer;
            this.v = v;
        }

        @Override
        public void run() {
            producer.onData(v);
        }
    }
}
