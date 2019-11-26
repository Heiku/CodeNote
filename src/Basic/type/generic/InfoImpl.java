package Basic.type.generic;

/**
 * @Author: Heiku
 * @Date: 2019/7/14
 */
public class InfoImpl implements Info<String> {

    private String var;

    public InfoImpl(String var){
        this.setVar(var);
    }

    @Override
    public void setVar(String var) {
        this.var = var;

    }

    @Override
    public String getVar() {
        return var;
    }

    public static void main(String[] args) {
        InfoImpl i = new InfoImpl("Da Vinci");
        System.out.println(i.getVar());
    }
}
