package Baisc.lambda;

/**
 * @Author: Heiku
 * @Date: 2019/7/26
 *
 *
 * lambda: Lambda表达式被封装成了主类的一个私有方法，并通过invokedynamic指令进行调用。
 */
public class MainAnonymousClass {
    public static void main(String[] args) {
        new Thread(new Runnable(){
            @Override
            public void run(){
                System.out.println("Anonymous Class Thread run()");
            }
        }).start();;
    }
}
