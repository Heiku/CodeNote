package Basic.proxy.jdk;

import Basic.proxy.HelloService;
import Basic.proxy.HelloServiceImpl;

import java.lang.reflect.Proxy;

/**
 * @Author: Heiku
 * @Date: 2019/9/26
 */
public class JDKInvocation {
    public static void main(String[] args) {
        HelloService service = new HelloServiceImpl();

        JDKHandler handler = new JDKHandler(service);

        service = (HelloService) Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(),
                new Class[]{HelloService.class}, handler);

        service.hello();
    }
}
