
### 进程 & 线程

进程是一个独立的运行环境，而线程是在进程中执行的一个任务。他们本质的区别是：__是否单独占有内存地址空间及其他
系统资源（I/O）__

* 进程单独占有一定的内存地址空间，所以进程间存在内存隔离，数据是分开的，数据共享复杂但是同步简单，各个进程之间互不干扰；
而线程共享所属进程占有的内存空间地址和资源，数据共享简单，但是同步复杂。

* 进程单独占有一定的内存地址空间，一个进程如果出现问题不会影响其他进程，不影响主程序的稳定性，可靠性高；一个线程崩溃
可能影响整个程序的稳定性，可靠性较低。

* 进程单独占有一定的n内存地址空间，进程的创建和销毁不仅需要保存寄存器和栈信息，还需要资源的分配回收以及页调度，
开销较大；线程只需要保存寄存器和栈信息，开销较小。

__进程是操作系统进行资源分配的基本单位，而线程是操作系统进行调度的基本单位，即 CPU 分配时间的单位__

#### 上下文切换

上下文：某一个时间点 __CPU 寄存器__ 和 __程序计数器__ 的内容

* 寄存器：CPU 内部的少量运行速度块的闪存，通常存储和访问计算过程的中间值提高计算机程序的运行速度。

* 程序计数器：用于表明指令序列中 CPU 正在执行的位置，存的值为正在执行的指令的位置或者下一个指令的位置。


![](/img/thread-state.webp)

### 同步 & 异步 & 阻塞 & 非阻塞

同步异步关注的是结果消息的通信机制：
* 同步：调用方需要主动等待结果的返回。（因为不知道结果怎么样，所以需要不断轮询获取结果信息）
* 异步：调用方不需要主动等待结果的返回。通过其他手段实现如：状态通知，回调函数

阻塞非阻塞关注的是等待结果的过程中调用方的状态：
* 阻塞：结果返回前，当前线程挂起，不做任何事
* 非阻塞：结果返回之前，线程可以可以做其他事情，不会被挂起


### 线程状态

Java 中的线程状态如下：

* 开始 
* 就绪 start, notify, resume
* 运行 
* 阻塞 wait, sleep, suspend
* 终止 stop

#### Thread.sleep()

sleep 是告诉操作系统自己休息 n ms.

1. Thread.sleep(0) 进入就绪状态  
如果 n = 0, 意味着当前的线程放弃当前剩下的时间片，进入就绪状态，这时候，更高优先级的线程将会运行，而优先级低的线程将的不到
运行。如果没有符合条件的线程，会一直占用 CPU 时间片，造成 CPU 100% 占用率

2. Thread.sleep(1) 进入阻塞状态
如果 n > 0，会强制当前线程放弃剩余时间片，并休息 n ms（非精确时间，会延时），进入阻塞状态。这种情况下，所有其他就绪状态
的线程都有机会竞争时间片，而不用在乎优先级。无论有没有符合的线程，都会放弃CPU时间，因此 CPU 占用低。


#### Thread.yield()

yield 是让当前的线程主动让出时间片，并让操作系统调度其他就绪态的线程使用时间片。  
当前线程会被放入到就绪队列 而不是 阻塞队列，如果没有找到其他就绪状态的线程，则当前的线程继续运行。
可替代 `Thread.sleep(0)`.

#### sleep wait

1. sleep 属于 Thread，而每个对象 Object 都可调用 wait
2. sleep 不会释放锁，而 wait 会释放对象锁，进入对象的等待锁定区


#### wait notify notifyAll

1. wait(), notify(), notifyAll() 都需要先获取对象锁，调用 wait() 会释放锁
2. wait()调用后，线程的状态由 `RUNNING` 变成 `WAITING`，并将当前线程放置到对象的等待队列中
3. notify() / notifyAll() 方法调用后，等待的线程并不会立即从 wait() 中返回，需要等该线程释放锁之后，才有机会获得锁
之后从 wait() 返回
4. notify() 方法将等待队列中的等待线程从等待队列中移至同步队列中，被移动的线程状态从 `WAITING` 变成 `BLOCKED`
5. 从 wait() 方法返回的前提是，改线程获得了调用对象的锁

#### Monitor

在 HotSpot 虚拟机中，monitor 采用 ObjectMonitor 实现。

每个线程都有两个 ObjectMonitor 对象列表，分别为 free 和 used 列表，如果当前的 free 列表为空，线程将向全局 global list 请求请求分配
ObjectMonitor。

ObjectMonitor 对象中有两个队列: _WaitSet 和 _EntryList，用来保存 ObjectWaiter 对象列表；_owner 用来指向获得 ObjectMonitor 对象的
线程。
* WaitSet: 处于 wait 状态的线程，会被加入到 wait set中
* EntryList: 处于等待锁 blocked 状态的线程，会被加入到 entry set

当多个线程访问一段同步代码时，会通过 cas 将 _owner 设置为当前线程，如果失败的线程将会被挂起 block 并加入到 _entryList 列表中，
如果调用 `wait()` 将会将释放当前线程持有的 monitor，并封装 node 加入到 _WaitSet 中，等待被唤醒。

##### Wait()

`ObjectMonitor.wait(jlong millis, bool interruptable, TRAPS)`

1. 将当前 __线程__ 封装成 ObjectMonitor 对象 node
2. 通过 `ObjectMonitor :: AddWaiter` 将 node 添加到 _WaitSet 中
3. 通过 `ObjectMonitor :: exit` 释放当前的 ObjectMonitor 对象，这样其它竞争线程就可以获取该 ObjectMonitor 对象
4. 最终底层的 `park()` 会挂起线程

#### notify()

`ObjectMonitor.notify(TRAPS)`

1. 如果当前 _WaitSet 为空，即没有正在等待的线程，则直接返回
2. 通过 `ObjectMonitor :: DequeueWaiter` 方法，获取 _WaitSet 列表中的第一个 ObjectWaiter 节点
3. 根据不同的策略，将取出来的 ObjectWaiter 节点，加入到 _EntryList 或者直接通过 `Atomic :: cmpcxg_ptr` 指令
进行自旋 cxq 竞争锁


#### 线程中断

线程中断有两种状态：运行时中断、阻塞时中断 [ThreadInterrupt](/src/Basic/thread/ThreadInterruptWithLock.java)
* 运行时中断：线程运行期间，中断线程，对线程毫无影响，只能通过标志位来判断
* 阻塞时中断：一种是等待锁时中断，另一种时阻塞时中断，持有锁了 wait() 中断，[WaitNotify](/src/Basic/thread/waitnotify/WaitNotify.java)

1. synchronized: 阻塞式的，无法响应中断
2. wait()： 声明了中断异常，如果等待过程中，执行了 Thread.interrupt()，那么将会抛出异常
3. Lock.lock()：lock() 不会响应异常，lockInterrupt() 则会响应异常。而 tryLock() 不会抛出异常，tryLock(time, unit) 则会抛出异常,
他们抛出异常。异常抛出的原理是 `LockSupport.park()`，该方法在线程中断后立即返回不再阻塞，不抛异常
4. Condition.await(): 方法响应异常，并抛出异常。而 awaitUnInterruptibly 则不会响应异常。原理也是 LockSupport
5. LockSupport.part(): 线程中断后将不再阻塞，也不抛异常
6. Thread.join(): join() 内部实际上是调用 wait()，自然响应异常
7. Thread.sleep(): 方法内部是由 JVM 实现，会响应异常并抛出


### threadLocal

threadLocal 的内存泄漏问题？

在 threadLocal.threadLocalMap.Entry 中，key 被保存在 WeakReference 对象中，导致了当 threadLocal 没有外部强引用时，发生
GC 时回收，如果创建 ThreadLocal 的线程一直运行，那么这个 Entry 对象中的 value 就有可能一直的不到回收，发生内存泄漏。
所以建议在使用完 threadLocal 后，在finally 中调用 remove()。

### 引用

[JVM源码分析之Object.wait/notify实现](https://www.jianshu.com/p/f4454164c017)  
[Thread.sleep and Thread.yield](https://www.jianshu.com/p/b65a7eba937d)  
[深入浅出synchronized](https://www.jianshu.com/p/19f861ab749e)
 