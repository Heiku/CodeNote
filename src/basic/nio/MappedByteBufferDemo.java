package basic.nio;

import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @Author: Heiku
 * @Date: 2019/11/6
 */
public class MappedByteBufferDemo {

    public static void main(String[] args) throws Exception {
        FileChannel fileChannel = new RandomAccessFile("C:\\Users\\DELL\\Desktop\\note.txt", "rw").getChannel();


        // 这里可以直接映射磁盘中的文件大小
        MappedByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, 1 * 1024 * 1024 * 1024);

        byte[] data = new byte[4];
        int position = 0;

        // write 4 byte
        mappedByteBuffer.position(0);
        mappedByteBuffer.put(data);
        mappedByteBuffer.put("Hello World !".getBytes());

        // write with position
        mappedByteBuffer.position(0);
        mappedByteBuffer.put(data);
        // MappedByteBuffer buffer = (MappedByteBuffer)mappedByteBuffer.slice();


        // read data
        /*mappedByteBuffer.position(0);
        mappedByteBuffer.get(data);*/

    }
}
