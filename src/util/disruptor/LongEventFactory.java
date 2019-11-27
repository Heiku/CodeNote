package util.disruptor;

import com.lmax.disruptor.EventFactory;

/**
 * @Author: Heiku
 * @Date: 2019/11/27
 */
public class LongEventFactory implements EventFactory<LongEvent> {
    @Override
    public LongEvent newInstance() {
        return new LongEvent();
    }
}
