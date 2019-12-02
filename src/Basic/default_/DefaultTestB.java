package Basic.default_;

/**
 * @Author: Heiku
 * @Date: 2019/9/17
 */
interface InterfaceBA{
    default void foo(){
        System.out.println("InterfaceA foo");
    }
}

interface InterfaceBB{
    default void bar(){
        System.out.println("InterfaceB bar");
    }
}

interface InterfaceBC{
    default void foo(){
        System.out.println("InterfaceC foo");
    }

    default void bar(){
        System.out.println("InterfaceC bat");
    }
}

class ClassA implements InterfaceA, InterfaceB {
}

public class DefaultTestB implements InterfaceBB, InterfaceBC {
    @Override
    public void bar() {
        InterfaceBB.super.bar(); // 调用 InterfaceB 的 bar 方法
        InterfaceBC.super.bar(); // 调用 InterfaceC 的 bar 方法
    }

    public static void main(String[] args) {
        // 如果想调用确切的接口方法，使用 Interface.super.method()
    }
}
