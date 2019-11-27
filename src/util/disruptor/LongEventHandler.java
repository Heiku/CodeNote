package util.disruptor;

import com.lmax.disruptor.EventHandler;

/**
 * @Author: Heiku
 * @Date: 2019/11/27
 */
public class LongEventHandler implements EventHandler<LongEvent> {

    @Override
    public void onEvent(LongEvent longEvent, long l, boolean b) throws Exception {
        System.out.println("consume event= [{" + longEvent.getValue() + "}]");
    }
}
