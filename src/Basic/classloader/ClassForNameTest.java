package Basic.classloader;

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
            Class.forName("Basic.classloader.ClassForName");
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
            ClassLoader.getSystemClassLoader().loadClass("Basic.classloader.ClassForName");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
