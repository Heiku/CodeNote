package Basic.proxy.cglib;

import Basic.proxy.HelloService;
import Basic.proxy.HelloServiceImpl;
import net.sf.cglib.proxy.Enhancer;

/**
 * @Author: Heiku
 * @Date: 2019/9/26
 */
public class CglibInvocation {
    public static void main(String[] args) {
        HelloService service = new HelloServiceImpl();

        Enhancer enhancer = new Enhancer();
        enhancer.setInterfaces(new Class[]{HelloService.class});
        enhancer.setCallback(new CglibInterceptor(service));

        service = (HelloService) enhancer.create();
        service.hello();
    }
}
