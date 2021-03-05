package basic.asm;

/**
 * @Author: Heiku
 * @Date: 2019/12/2
 */
public class ByteCodeDemo {

    private static final String name = "universe";

    private int age;

    public ByteCodeDemo(int age){
        this.age = age;
    }

    public int getAge(){
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public static void main(String[] args) {
        ByteCodeDemo demo = new ByteCodeDemo(18);
        System.out.println("name: " + name + "  age: " + demo.getAge());
    }
}
