package Basic.relect;

/**
 * @Author: Heiku
 * @Date: 2019/7/14
 */


public class Student {

    private String name;

    public Student(){
        System.out.println("创建了一个Student实例");
    }

    public Student(String name){
        //this.name = name;
        System.out.println("创建了一个Student实例");
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
