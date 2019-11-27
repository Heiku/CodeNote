package Basic.unsafe;

import lombok.Data;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * @Author: Heiku
 * @Date: 2019/11/27
 */
public class UnsafeCASTest {
    public static void main(String[] args) throws Exception {
        Unsafe unsafe = UnsafeI.getUnsafe();

        // use unsafe instance player object
        // but it will skipp construct method
        Player player = (Player) unsafe.allocateInstance(Player.class);
        player.setName("A Fei");
        player.setScore(11);
        for (Field f : Player.class.getDeclaredFields()) {
            System.out.println(f.getName() + " offset address: " + unsafe.objectFieldOffset(f));
        }
        System.out.println("--------");

        // scoreOffset is the offset of score field in player obj
        int scoreOffset = 12;

        // cas change field value with field offset
        System.out.println(unsafe.compareAndSwapInt(player, scoreOffset, 11, 20));
        System.out.println("after cas change score value: " + player.getScore());
        System.out.println("--------");

        // putOrderedInt change field, but not be seen for other thread,(use volatile instead of it)
        unsafe.putOrderedInt(player, 12, 30);
        System.out.println("after putOrderedInt change score value: " + player.getScore());
        System.out.println("--------");

        // putIntVolatile, safety change field value, it can be seen for other thread
        unsafe.putIntVolatile(player, 12, 50);
        System.out.println("after putIntVolatile change score value: " + player.getScore());


    }
}

@Data
class Player{
    private int score;
    private String name;
}
