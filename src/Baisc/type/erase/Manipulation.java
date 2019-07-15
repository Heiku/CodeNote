package Baisc.type.erase;

/**
 * @Author: Heiku
 * @Date: 2019/7/15
 *
 *
 * Java 的泛型是使用类型擦除，即使用泛型的时候，任何具体的类型信息都被擦除了
 */
/*
class Manipulator<T> {

    private T obj;

    public Manipulator(T x){
        obj = x;
    }

    public void manipulate(){
        //obj.f();          // 编译错误
    }

}
*/


// 给定了类型的上界，通过继承树实现的，对于继承树，父节点在上，子节点在下
class Manipulator<T extends HasF> {

    private T obj;

    public Manipulator(T x){
        obj = x;
    }

    public void manipulate(){
        obj.f();          // 编译错误
    }

}

public class Manipulation{
    public static void main(String[] args) {

        HasF hf = new HasF();
        Manipulator<HasF> manipulator = new Manipulator<>(hf);

        manipulator.manipulate();
    }
}
