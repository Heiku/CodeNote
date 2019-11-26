package Basic.spi;

/**
 * @Author: Heiku
 * @Date: 2019/11/4
 */
public class SpiServiceA implements SpiService {

    @Override
    public void hello() {
        System.out.println("SpiServiceA.hello");
    }
}
