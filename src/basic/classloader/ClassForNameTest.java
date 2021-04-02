package basic.classloader;

import org.junit.jupiter.api.Test;

/**
 * @Author: Heiku
 * @Date: 2019/11/26
 */

public class ClassForNameTest {


    /**
     * invoke static block
     * invoke static method
     *
     * load class into JVM, and init class in same time
     */
    @Test
    public void testClassForName(){
        try {
            for (int i = 0; i < 3; i++) {
                Class.forName("basic.classloader.ClassForName");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    /**
     * Just load class into JVM
     */
    @Test
    public void testClassLoader(){
        try {
            ClassLoader.getSystemClassLoader().loadClass("basic.classloader.ClassForName");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
