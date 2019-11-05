package Baisc.spi;

/**
 * @Author: Heiku
 * @Date: 2019/11/4
 */
public class SpiServiceB implements SpiService {

    @Override
    public void hello() {
        System.out.println("SpiServiceB.hello");
    }
}
