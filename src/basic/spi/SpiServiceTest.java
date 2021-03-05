package basic.spi;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * @Author: Heiku
 * @Date: 2019/11/4
 */
public class SpiServiceTest {

    public static void main(String[] args) {
        ServiceLoader<SpiService> serviceLoader = ServiceLoader.load(SpiService.class);

        Iterator<SpiService> iterator = serviceLoader.iterator();
        while (iterator.hasNext()){
            SpiService service = iterator.next();
            service.hello();
        }
    }
}
