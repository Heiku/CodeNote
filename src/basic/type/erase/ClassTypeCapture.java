package basic.type.erase;

/**
 * @Author: Heiku
 * @Date: 2019/7/15
 *
 *
 * 引入类型标签，使用动态的 isInstance() 代替 InstanceOF
 *
 * public class Erased<T> {
 *     private final int SIZE = 100;
 *     public static void f(Object arg) {
 *         if (arg instanceof T) {}     // 编译错误
 *         T var = new T();             // 编译错误
 *         T[] array = new T[SIZE];     // 编译错误
 *     }
 * }
 */

class Building {}
class House extends Building {}

public class ClassTypeCapture<T> {

    Class<T> kind;

    public ClassTypeCapture(Class<T> kind){
        this.kind = kind;
    }

    public boolean f(Object arg){
        return kind.isInstance(arg);
    }

    public static void main(String[] args) {

        ClassTypeCapture<Building> ct1 = new ClassTypeCapture<>(Building.class);
        System.out.println(ct1.f(new Building()));
        System.out.println(ct1.f(new House()));

        ClassTypeCapture<House> ct2 = new ClassTypeCapture<>(House.class);
        System.out.println(ct2.f(new Building()));
        System.out.println(ct2.f(new House()));
    }
}
