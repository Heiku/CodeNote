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
        // get cursor of ringBuffer
        long cursor = ringBuffer.getCursor();
        System.out.println("current cursor: " + cursor);

        // Grab the next sequence, apply the next seq num of ringBuffer
        // next(1), after the cursor
        long sequence = ringBuffer.next();
        System.out.println("put data sequence: " + sequence);

        try {
            // elementAt() -> unsafe getObject()
            LongEvent event = ringBuffer.get(sequence);
            event.setValue(v);
        }finally {
            // update cursor & signal All wait consumer thread
            ringBuffer.publish(sequence);
        }
    }
}
