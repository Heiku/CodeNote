package Basic.type.generic;

/**
 * @Author: Heiku
 * @Date: 2019/7/14
 */

// 接口定义泛型
public interface Info<T> {

    public T getVar();

    public void setVar(T var);

}
