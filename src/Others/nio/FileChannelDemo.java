package Others.nio;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;

/**
 * @Author: Heiku
 * @Date: 2019/5/20
 */

/**
 * 使用FileChannel 进行文件的追加写入
 *
 * NIO Buffer = capacity + position + limit
 *
 * flip() -> 设置 limit 为 position 的值，然后 position 置为0。对Buffer进行读取操作前调用，即从头开始读取
 *
 */
public class FileChannelDemo {
    public static void main(String[] args) {

        try {
            FileChannel channel = new RandomAccessFile("D:\\tmp\\filestore\\note.txt", "rw").getChannel();
            channel.position(channel.size());   // position 指向文件末尾

            ByteBuffer byteBuffer = ByteBuffer.allocate(100);
            byteBuffer.put("  new character by nio byteBuffer\n".getBytes(StandardCharsets.UTF_8));

            // Buffer data -> channel
            byteBuffer.flip();
            while (byteBuffer.hasRemaining()){
                channel.write(byteBuffer);
            }

            // 将 channel 指针移至头部
            channel.position(0);
            CharBuffer charBuffer = CharBuffer.allocate(100);
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();


            byteBuffer.clear();
            // channel -> byteBuffer
            while(channel.read(byteBuffer) != -1 || byteBuffer.position() > 0){
                byteBuffer.flip();

                // UTF-8 解码器
                charBuffer.clear();
                decoder.decode(byteBuffer, charBuffer, false);
                System.out.println(charBuffer.flip().toString());

                byteBuffer.compact();   // 处理剩余
            }

            channel.close();

        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
