## NIO

### 系统调用

读取一个文件的实际过程如下：  

1. 系统调用 read()，这时进行上下文的切换，从 user -> kernel， 然后 DMA 执行文件数据的拷贝，把文件数据将硬盘读取到了内核缓冲区中(kernel buffer)

2. 将数据从 kernel buffer 拷贝到 user buffer，然后系统调用 read() 返回，这时候又产生一个上下文切换，即 kernel -> user


### FileInputStream

1. 假设从一个文件中读取4字节的数据，user -> kernel，系统根据局部性原理，会额外加载数据到内核缓冲区中，这时，就会通过 DMA 读取12个字节到 kernel buffer中，
（这里涉及到一次上下文的切换，以及一次数据拷贝）

2. 然后再将 kernel buffer 中的4个字节拷贝到用户进行的缓冲区中 user buffer，这期间就伴随着 kernel -> user，这个时候 kernel buffer 中还保留着 8个字节的数据
（这里涉及一次上下文的切换，以及一次数据拷贝）

3. 当下次用户再次需要读取4个字节的数据的时候，就不需要等待 DMA 的拷贝，直接将 kernel buffer 拷贝到 user buffer，但这个过程伴随着 kernel -> user
（当再次需要数据的时候，还是会涉及上下文的切换）


### BufferedInputStream

1. same with FileInputStream

2. 用户需要4个字节，会从 kernel buffer 中拷贝 8个字节到 user buffer中，这时候 kernel buffer 中还剩4个字节，而 user buffer中还有4个暂未使用的数据字节

3. 当用户再次读取4个字节的时候，因为数据已经再user buffer中，所以可以直接读取user buffer，不需要切换上下文去 kernel buffer 中读取


### FileChannel

FileChannel 是借助了 PageCache，再操作读写文件的时候，FileChannel 借助 ByteBuffer 将数据读写进入磁盘，而 FileChannel 的性能主要是通过 PageCache 实现的。


### PageCache

PageCache 是 Linux 中对于文件的缓存，文件层面上的数据都会缓存到Page Cache中，提升读写访问的性能。

PageCache 是用户内存和磁盘中间的一层缓存，fileChannel.write() 写入 PageCache 完成落盘，但实际上 pageCache 中的数据何时刷入磁盘由操作系统决定，
在 FileChannel.force()中可以强制进行刷盘操作


为什么使用 PageCache 会快很多？

假设我们每次需要读取4k的数据，磁盘文件的大小为16k，pageCache大致为内存的20%  
当我们调用 fileChannel.read(4kb)，发生以下两件事：

1. 操作系统从磁盘加载了 16kb的数据到 pageCache 中，预读
2. 操作系统会从 PageCache 拷贝 4kb进入用户的内存

如果我们还需要访问的时候，那么这时候是进行 4次磁盘IO 还是进行 一次磁盘IO + 4次内存IO 呢？  
结果就很明显了

预读：当一个文件被读取的时候，在它临近扇区所存储的文件数据也在近期b被读取，所以硬盘会预先读取到后者的缓存中，以便不久的将来，
当这些数据被请求的时候，就可以直接从缓存中向外输送数据


### MappedBuffered

MappedBuffered 是底层原理是 Linux 中的 mmap()，即通过内存文件映射，使得用户可以直接通过对用户进程上的文件指针进行修改，修改的内容最终会被映射到磁盘文件中

mmap：mmap 把文件映射到用户空间里的虚拟内存中，省去了从内核缓冲区复制到用户空间的过程，文件中的磁盘位置在用户进程的虚拟内存空间中有了对应的地址，可以像操作内存
一样操作这个文件，相当于已经把整个文件放入到内存中，但真正使用到这些数据却不会消耗物理内存，也不会有读写磁盘的操作，只有真正使用这些数据的时候，也就是图像准备渲染
在屏幕上时，虚拟内存管理系统 VMS 才会根据缺页加载的机制从磁盘中加载对应的物理块到物理内存中渲染，这样的读写文件的方式少了数据从内核缓存到用户缓存空间的拷贝，效率很高


使用注意：

1.因为内存的最小粒度是页，而进程虚拟地址空间和内存的映射也是以页为单位，为了匹配内存的操作，所以mmap 从磁盘到虚拟地址空间的映射也必须为页。
mmap 映射区域的大小必须是物理页大小（page size）的整数倍（n * 4k），同时为了减小重复mmap 带来的虚拟内存回收，重新分配的问题，
所以一般的取值大小都在会大于1G（RocketMQ commmitlog 默认大小）

2.mmap 使用的是虚拟内存，和 pageCache 一样都是由系统负责刷盘的，虽然可以通过 force()，但时间不好把握，在小内存场景下会会由性能的问题

3.mmap 的回收问题




### 引用

[文件 IO 操作的一些最佳实践](https://www.cnkirito.moe/file-io-best-practise/)  
[高性能IO 之 内存映射 mmap的理解](https://www.callmejiagu.com/2018/12/21/%E5%86%85%E5%AD%98%E6%98%A0%E5%B0%84-mmap%E7%9A%84%E7%90%86%E8%A7%A3/)