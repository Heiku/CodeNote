package basic.proxy;

/**
 * @Author: Heiku
 * @Date: 2019/9/26
 */
public class HelloServiceImpl implements HelloService {

    @Override
    public void hello() {
        System.out.println("Hello World!");
    }
}
