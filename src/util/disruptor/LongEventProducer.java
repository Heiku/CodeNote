package util.disruptor;

import com.lmax.disruptor.RingBuffer;

/**
 * @Author: Heiku
 * @Date: 2019/11/27
 */
public class LongEventProducer {

    private final RingBuffer<LongEvent> ringBuffer;

    public LongEventProducer(RingBuffer<LongEvent> ringBuffer){
        this.ringBuffer = ringBuffer;
    }

    public void onData(long v){
        long cursor = ringBuffer.getCursor();
        System.out.println("current cursor: " + cursor);

        // Grab the next sequence
        long sequence = ringBuffer.next();
        System.out.println("put data sequence: " + sequence);

        try {
            LongEvent event = ringBuffer.get(sequence);
            event.setValue(v);
        }finally {
            ringBuffer.publish(sequence);
        }
    }
}
