package Basic.type.generic;

/**
 * @Author: Heiku
 * @Date: 2019/7/14
 */
public class InfoImplT<T> implements Info<T>  {

    private T var;

    public InfoImplT(T var){
        this.setVar(var);
    }

    @Override
    public T getVar() {
        return var;
    }

    @Override
    public void setVar(T var) {
        this.var = var;
    }

    public static void main(String[] args) {
        InfoImplT<String> i = new InfoImplT<>("multi");
        System.out.println(i.getVar());
    }
}
