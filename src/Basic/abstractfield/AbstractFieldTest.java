package Basic.abstractfield;

/**
 * @Author: Heiku
 * @Date: 2019/12/3
 */
public class AbstractFieldTest {
    public static void main(String[] args) {
        SubClass c1 = new SubClass("sub1", "base1");
        SubClass c2 = new SubClass("sub2", "base2");

        System.out.println(c1);
        System.out.println(c2);
    }
}
