package basic.abstractfield;

/**
 * @Author: Heiku
 * @Date: 2019/12/3
 */
public class SubClass extends AbstractBase {

    private String desc;

    public SubClass(String desc, String name){
        this.desc = desc;
        this.name = name;
    }

    @Override
    public String toString() {
        return "SubClass{" +
                "desc='" + desc + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
