package Baisc.proxy.cglib;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * @Author: Heiku
 * @Date: 2019/9/26
 */
public class CglibInterceptor implements MethodInterceptor {

    Object object;

    public CglibInterceptor(Object object) {
        this.object = object;
    }

    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        System.out.println("before invoke");
        methodProxy.invoke(object, objects);
        System.out.println("after invoke");

        return null;
    }
}
