package Basic.proxy.jdk;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @Author: Heiku
 * @Date: 2019/9/26
 */
public class JDKHandler implements InvocationHandler {

    Object object;

    JDKHandler(Object o){
        this.object = o;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("before invoke");
        method.invoke(object, args);
        System.out.println("after invoke");

        return null;
    }
}
