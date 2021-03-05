package basic.type.virtual;

/**
 * @Author: Heiku
 * @Date: 2019/12/17
 */
public class DynamicDispatch {
    static abstract class Human {
        abstract void say();
    }

    static class Man extends Human {
        @Override
        void say() {
            System.out.println("hello, man");
        }
    }

    static class Woman extends Human {
        @Override
        void say() {
            System.out.println("hello, woman");
        }
    }

    public static void main(String[] args) {
        Human man = new Man();
        Human woman = new Woman();

        // dynamic dispatch
        // method overload, it depend on variable real type
        man.say();
        woman.say();
        //  hello, man
        //  hello, woman
        // 17: invokevirtual #6                  // Method Basic/type/virtual/DynamicDispatch$Human.say:()V
        // 21: invokevirtual #6                  // Method Basic/type/virtual/DynamicDispatch$Human.say:()V

    }
}
