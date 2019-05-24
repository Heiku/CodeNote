package Baisc.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * @Author: Heiku
 * @Date: 2019/5/20
 */
public class NIOServer {
    public static void main(String[] args) {
        try {
            Selector selector = Selector.open();

            // 初始化 TCP 连接监听器
            ServerSocketChannel listenChannel = ServerSocketChannel.open();
            listenChannel.bind(new InetSocketAddress(8081));
            listenChannel.configureBlocking(false);     // 配置为非阻塞式

            // 注册到 selector 上
            listenChannel.register(selector, SelectionKey.OP_ACCEPT);

            // ByteBuffer
            ByteBuffer byteBuffer = ByteBuffer.allocate(100);

            while (true){
                // Selects a set of keys whose corresponding channels are ready for I/O
                selector.select();      // 当前线程阻塞， 直到监听的事件发生，向下执行 (选择有I/O 事件的管道信息 (key))
                Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();

                // 迭代访问select 得到的 channel 事件
                while (keyIter.hasNext()) {
                    SelectionKey key = keyIter.next();

                    // 建立连接， 将connect socket channel 注册到 selector上
                    if (key.isAcceptable()) {
                        SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
                        socketChannel.configureBlocking(false);
                        socketChannel.register(selector, SelectionKey.OP_READ);

                        System.out.println("与 [ " + socketChannel.getRemoteAddress() + "] 建立连接 ！");
                    } else if (key.isReadable()) {    // 可读取数据
                        byteBuffer.clear();

                        // channel -> buffer
                        // 读取到channel末尾说明TCP连接断开
                        // 因此需要关闭channel， 或者取消监听 READ 事件
                        // 否则进入死循环
                        if (((SocketChannel)key.channel()).read(byteBuffer) == -1){
                            key.channel().close();
                            continue;
                        }

                        // 读取channel数据
                        byteBuffer.flip();
                        while (byteBuffer.hasRemaining()){
                            byte b = byteBuffer.get();

                            // 客户端消息结束符 \0
                            if (b == 0){
                                System.out.println();

                                // 响应客户端
                                byteBuffer.clear();
                                byteBuffer.put("Hello, Client ！\n".getBytes());
                                byteBuffer.flip();

                                while (byteBuffer.hasRemaining()){
                                    // buffer -> channel
                                    ((SocketChannel) key.channel()).write(byteBuffer);
                                }
                            }else {
                                System.out.print((char)b);
                            }
                        }
                    }

                    keyIter.remove();
                }
            }

        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
