package basic.asm;

/**
 * javap: 根据 class字节码文件，反解析出当前类的 code区，本地变量表，异常表 和 代码行偏移量映射表，常量池等信息
 *
 * @Author: Heiku
 * @Date: 2019/12/2
 */
public class LocalVariableTableDemo {
    public static void main(String[] args) {
        // Code:
        // stack=3, locals=5, args_size=1
        // 4 int slot + this slot
        int a = 1, b = 1, c = 1, d = 1;
        System.out.println(a + ' ' + b + ' ' + c + ' ' + d);
    }
}
