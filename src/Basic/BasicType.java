package Basic;

/**
 * 静态分派 vs 动态分派
 *
 *
 *  静态分派：所有依赖静态类型来定位方法执行版本的分派动作，称为静态分派，在编译期间确定，应用：方法重载
 *  动态分派：根据实际类型的变化定位方法，在程序运行时得知。应用：方法重写
 *
 *
 *  boolean(1/8) byte(1) char(2) int(4) short(2) long(8) float(4) double(8)
 *  参数的自动类型转换：
 *      char -> int -> long -> float -> double
 *
 *
 *  字节码指令调用：
 *
 *  invokestatic:       调用静态方法
 *  invokespecial:      调用实例的构造器<init>方法，私有方法和父类方法
 *  invokevirtutal:     调用所有的虚方法
 *  invokeinterface:    调用接口方法，会在运行时再确定一个实现此接口的对象
 *  invokedynamic:      先在运行时动态解析出调用点限定符所引用的方法，再执行该方法，在此之前调用的4条调用指令，分派逻辑是固化在 JVM 内部的，
 *                      而 invokedynamic 是由用户所设定的引导方法决定的
 *
 *
 * @Author: Heiku
 * @Date: 2019/10/18
 */
public class BasicType {
    public static void main(String[] args) {


        // 这里的 Base 称为静态类型( Static Type / Apparent Type)，Child 称为实际类型（Actual Type）
        // 0: new           #2                  // class Baisc/Child
        // 4: invokespecial #3                  // Method Baisc/Child."<init>":()V
        Base child = new Child();

        System.out.println("first print");
        // 这里因为无参数，而 Base 中存在对应的方法 foo() ，所有只能是由 Base.foo()
        // 18: invokestatic  #7                  // Method Baisc/Base.foo:()V
        child.foo();
        // 因为子类 Child 中存在对应的 bar(Character c)，所有确定为 Child.foo(Character c)
        // 31: invokevirtual #10                 // Method Baisc/Base.bar:(Ljava/lang/Character;)V
        child.bar(new Character('C'));

        System.out.println("\nsecond print");
        Object integer = new Integer(100);
        // 在在静态分配时，是根据传入方法的参数的静态类型来决定调用的方法版本, 实际传入的参数为 Integer 的静态类型 Object，所以调用 Base.baz(Object)
        //  54: invokevirtual #14                 // Method Baisc/Base.baz:(Ljava/lang/Object;)V

        // 这里是动态分派的一个体现：方法重写
        // 简要分析一下指令 invokevirtual 的调用过程
        // 1. #14 代表 BasicType 这个类常量池中第14个常量表的索引，常量表( CONSTANT_Methodref_info)中记录了方法信息即 baz()的符号引用信息，再根据这个符号引用找到调用对象 child 的类对象 Base
        // 2. 在 Base 的方法表中查找方法 baz()， 如果找到的话，将方法 baz() 在方法表中的索引值index 记录到 BasicType 类常量池中第 14 个常量池中（常量池解析）
        // 3. 在调用 invokevirtual 之前，采用 aload_1, 将开始创建的堆中的 Child 对象的引用压入操作数栈中，然后通过 invokevirtual 指令根据这个 Child 对象的引用找到堆中的 Child对象，
        //     然后进一步找到 Child对象所属的方法表
        // 4. 通过(2) 中的index，找到 Child 的方法表中的方法 baz()，然后通过直接地址找到该字节码所在的内存空间
        child.baz(integer);


        System.out.println("\nthird print");
        //
        // 68: invokevirtual #16                 // Method Baisc/Base.bar:(I)V
        // 简要分析：
        // 这里和上面一样，先找到对应的 Base 中的方法 bar()，虽然Base 中没有对应的 bar(char c)，但发生参数类型自动转换，由编译器找到一个 ”合适的调用“，即bar(int c)
        // 到子类Child 的时候，因为参数已经被转换成了 int， 导致只有 Base 中的 bar()适配
        child.bar('C');


        System.out.println("\njust print");
        // 这静态分派，依赖静态类型来定位方法执行版本的分派动作
        // 通过静态类型 Base 确定调用 print(Base)
        Printer.print(child);
    }
}


class Base{
    public static void foo(){
        System.out.println("Base.foo() invoked");
    }

    public void bar(int c){
        System.out.println("Base.bar(int) invoked");
    }

    public void bar(Character c){
        System.out.println("Base.bar(Character) invoked");
    }

    public void baz(Object o){
        System.out.println("Base.baz(Object) invoked");
    }

    public void baz(Integer i){
        System.out.println("Base.baz(Integer) invoked");
    }
}

class Child extends Base{
    public static void foo(){
        System.out.println("Child.foo() invoked");
    }

    public void bar(Character c){
        System.out.println("Child.bar(Character) invoked");
    }

    public void bar(char c){
        System.out.println("Child.bar(char) invoked");
    }
}

class Printer{
    public static void print(Base object){
        System.out.println("this is base object");
    }

    public static void print(Child object){
        System.out.println("this is child object");
    }
}


