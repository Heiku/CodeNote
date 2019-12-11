

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



### 线程池

为什么线程资源那么“珍贵”：

1. 线程的创建和销毁的成本很高，对于Linux系统来说，线程本质上就是一个进程，创建和销毁都是调用重量级的系统函数

2. 线程本身占用较多内存，像Java的堆栈，一般至少分配 512K ~ 1M 的空间，如果线程过多，线程占用的内存将会占用 JVM的一半以上

3. 线程的切换成本是很高的，当系统进行线程切换的时候，需要保留线程的上下文，然后执行系统调用，
如果系统的线程数过高，线程的切换时间可能会超过线程的执行时间，这时候带来的是系统load偏高，CPU sys使用率上升，导致系统不可用状态

4. 容易造成锯齿形的系统负载，因为系统负载是用活动线程数或 CPU核心数，一旦线程数量高但外部网络不稳定时，容易造成大量请求的结果同时返回，
激活大量阻塞线程从而使系统负载压力过大
 

### 引用

[Thread.sleep and Thread.yield](https://www.jianshu.com/p/b65a7eba937d)  
[深入浅出synchronized](https://www.jianshu.com/p/19f861ab749e)
 