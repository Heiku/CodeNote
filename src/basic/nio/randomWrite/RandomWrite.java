package basic.nio.randomWrite;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Author: Heiku
 * @Date: 2019/11/6
 */
public class RandomWrite {

    public static FileChannel fileChannel;
    public static AtomicLong wrotePosition;

    public static void main(String[] args) throws Exception {
        fileChannel = new RandomAccessFile("C:\\Users\\DELL\\Desktop\\random.txt", "rw").getChannel();

        ExecutorService executor = Executors.newFixedThreadPool(64);
        wrotePosition = new AtomicLong();
        for (int i = 0; i < 1024; i++){
            final int index = i;
            executor.execute(() -> {
                try {
                    fileChannel.write(ByteBuffer.wrap(new byte[4 * 1024]), wrotePosition.getAndAdd(4 * 1024));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
