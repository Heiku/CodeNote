package Basic.nio;

/**
 * @Author: Heiku
 * @Date: 2019/5/20
 */

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;

/**
 * 问题：为什么需要 Selector ?
 *
 *      答：传统I/O 是阻塞式I/O ，当读取一个 TCP连接的数据时，调用 InputStream 的 read()， 当没有数据的时候，当前线程会挂起，直到数据到达。
 *          为了能够读取其他连接的数据，（通常是采用线程池）会开辟很多线程去处理，当并发量一大时，系统的内存资源将会被大量线程消耗殆尽。
 *          同时，多线程之间的线程切换会去频繁更改处理器的状态，如程序计数器，寄存器值等，占用资源
 *
 *          而 NIO 是非阻塞式 I/0， read()方法在没有读取数据时会立即返回，导致不知道何时数据到达，只能不停的调用 read()进行重试，消耗CPU
 *
 *          在非阻塞模式下，通过 Selector，线程只为已经就绪的通过工作。比如，当所有的通过都没有数据到达时，就没有 Read事件发生，
 *          线程在 select() 方法被挂起，从而让出 CPU 资源。
 *
 *
 *      Selector 的4个监听事件：
 *          Accept：有可以接受的事件
 *          Connect：连接成功
 *          Read：有数据可读
 *          Write：可以写入数据
 */
public class SelectorDemo {
    public static void main(String[] args) throws IOException {
        Selector selector = Selector.open();

        ServerSocketChannel listenChannel = ServerSocketChannel.open();
        listenChannel.bind(new InetSocketAddress(9999));
        listenChannel.configureBlocking(false);

        /**
         * 注册到selector（监听其ACCEPT事件）
         * 这里的 parm 为
         *      OP_READ = 1 << 0
         *      OP_WRITE = 1 << 1
         *      OP_CONNECT = 1 << 2
         *      OP_ACCEPT = 1 << 0
         *
         * 判断是否包含指定的状态只需 & OP_READ， 进行位运算进行比较
         *
         *      public final boolean isReadable() {
         *         return (readyOps() & OP_READ) != 0;
         *     }
         */
        SelectionKey key = listenChannel.register(selector, SelectionKey.OP_ACCEPT);        // 返回的是本次的注册信息

    }
}
