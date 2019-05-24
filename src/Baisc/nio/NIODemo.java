package Baisc.nio;

/**
 * @Author: Heiku
 * @Date: 2019/5/20
 *
 * NIO 面向缓冲区，对 data 的操作基本通过 buffer 进行
 * IO  面向流， 对 data 的操作通过 stream
 *
 * NIO 主要由 Channel + Buffer + Selector
 *
 * 好处：
 *      1.事件驱动模型
 *      2.避免多线程
 *      3.单线程处理多任务
 *      4.非阻塞I/O，I/O 读写不再阻塞，而是返回0
 *      5.基于block的传输，通常比流的传输更高效
 *      6.更高级的IO 函数，zero-copy
 *      7.IO 的多路复用模型大大提高了Java网络应用的可伸缩性和实用性
 */

public class NIODemo {
}
