package basic.type.erase;

/**
 * @Author: Heiku
 * @Date: 2019/7/15
 *
 *
 * 模板方法
 */

abstract class GenericWithCreate<T>{
    final T element;

    public GenericWithCreate(){
        element = create();
    }

    abstract T create();
}

class X{

}


class Creator extends GenericWithCreate<X>{

    X create(){
        return new X();
    }

    void f(){
        System.out.println(element.getClass().getSimpleName());
    }
}

public class CreatorGeneric {
    public static void main(String[] args) {
        Creator c = new Creator();
        c.f();
    }
}
