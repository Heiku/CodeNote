package basic.type.virtual;

/**
 * hello, human
 * hello, human
 *
 * @Author: Heiku
 * @Date: 2019/12/17
 */
public class StaticDispatch {
    static abstract class Human {
    }

    static class Man extends Human {
    }

    static class Woman extends Human {
    }

    public void hello(Human guy) {
        System.out.println("hello, human");
    }

    public void hello(Man guy) {
        System.out.println("hello, man");
    }

    public void hello(Woman guy) {
        System.out.println("hello, woman");
    }

    public static void main(String[] args) {
        Human man = new Man();
        Man realMan = new Man();
        Human woman = new Woman();

        // static dispatch
        // method overload, it will depend on variable reference type
        StaticDispatch dispatch = new StaticDispatch();
        dispatch.hello(man);
        dispatch.hello(woman);
        dispatch.hello(realMan);

        //  hello, human
        //  hello, human
        //  hello, man
        // 26: invokevirtual #13                 // Method hello:(LBasic/type/virtual/StaticDispatch$Human;)V
        // 31: invokevirtual #13                 // Method hello:(LBasic/type/virtual/StaticDispatch$Human;)V
    }
}
