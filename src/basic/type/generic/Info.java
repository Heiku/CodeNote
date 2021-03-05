package basic.type.generic;

/**
 * @Author: Heiku
 * @Date: 2019/7/14
 */

// 接口定义泛型
public interface Info<T> {

    T getVar();

    void setVar(T var);

}
