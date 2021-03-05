package basic.default_;

/**
 * @Author: Heiku
 * @Date: 2019/9/17
 */

interface InterfaceA {
    default void foo() {
        System.out.println("InterfaceA foo");
    }
}

interface InterfaceB extends InterfaceA {

}

interface InterfaceC extends InterfaceA {
    @Override
    default void foo() {
        System.out.println("InterfaceC foo");
    }
}

interface InterfaceD extends InterfaceA {
    @Override
    void foo();
}


public class DefaultTestA {
    public static void main(String[] args) {

        new InterfaceB() {
        }.foo();       // InterfaceA foo
        new InterfaceC() {
        }.foo();       //  InterfaceC foo
        new InterfaceD() {
            @Override
            public void foo() {
                System.out.println("InterfaceD foo");
            }
        }.foo();                        // InterfaceD foo
    }
}
