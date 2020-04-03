
### 线程池

为什么线程资源那么“珍贵”：

1. 线程的创建和销毁的成本很高，对于Linux系统来说，线程本质上就是一个进程，创建和销毁都是调用重量级的系统函数

2. 线程本身占用较多内存，像Java的堆栈，一般至少分配 512K ~ 1M 的空间，如果线程过多，线程占用的内存将会占用 JVM的一半以上

3. 线程的切换成本是很高的，当系统进行线程切换的时候，需要保留线程的上下文，然后执行系统调用，
如果系统的线程数过高，线程的切换时间可能会超过线程的执行时间，这时候带来的是系统load偏高，CPU sys使用率上升，导致系统不可用状态

4. 容易造成锯齿形的系统负载，因为系统负载是用活动线程数或 CPU核心数，一旦线程数量高但外部网络不稳定时，容易造成大量请求的结果同时返回，
激活大量阻塞线程从而使系统负载压力过大
 

#### workQueue

阻塞队列用来保存被执行的任务，且存储的元素（任务）必须实现 Runnable 接口：

1. ArrayBlockingQueue: 基于数组结构的有界阻塞队列，按 FIFO 排序任务
2. LinkedBlockingQueue: 基于链表结构的阻塞队列，按 FIFO 排序任务，吞吐量通常要高于 ArrayBlockingQueue
3. SynchronousQueue: 一个不存储元素的阻塞队列，每个插入操作必须等待另一个线程调用移除操作，否则插入操作一直处于阻塞状态，
吞吐量通常要高于 LinkedBlockingQueue
4. PriorityBlockingQueue: 具有优先级的无界阻塞队列

#### handler

线程池的饱和策略，当工作队列已满，且没有空闲的线程处理，如果继续提交任务，必须采取一种策略处理该任务：

1. AbortPolicy: 直接抛出异常，默认策略
2. CallerRunsPolicy: 用调用者所在的线程在执行任务
3. DiscardOldestPolicy: 丢弃阻塞队列中靠最前的任务，并执行当前任务
4. DiscardPolicy: 直接丢弃任务

也可以实现 RejectedExecutionHandler 接口，自定义饱和策略，如记录日志或持久化不能处理的任务

#### Executors

1. newFixedThreadPool(n)

初始化一个指定线程数的线程池，其中 corePoolSize == maximumPoolSize，使用 LinkedBlockingQueue 作为阻塞队列，
不过当线程池中没有可执行的任务使，不会释放线程，一直保持着 corePoolSize

2. newCachedThreadPool()

* 初始化一个可以缓存线程的线程池，默认缓存60s，线程池的线程数可达到 Integer.MAX_VALUE，内部使用 SynchronousQueue 作为阻塞队列
* 与 newFixedThreadPool 不同，newCachedTheadPool 在没有任务执行的时候，当线程的空闲时间超过 keepAliveTime，会自动释放
线程资源，当提交任务的时候，如果没有空闲线程，则需要创建新线程执行任务，会导致一定的系统开销

所以在使用 CachedThreadPool 的时候，一定要注意控制并发的任务数，否则创建大量的线程可能导致严重的性能问题

3. newSingleThreadExecutor()

初始化的时候 singleThreadExecutor 中只有一个线程，如果该线程异常结束，会重新创建一个新的线程继续执行任务，唯一的线程
可以保证所有提交任务的顺序执行，内部使用 LinkedBlockingQueue 作为阻塞队列

4. newScheduledThreadPool(coreSize)

ScheduledThreadPool 可以再指定的时间内周期性的执行所提交的任务，在实际的业务场景中可以使用该线程池定期的同步数据

5. newWorkStealingPool()

workStealingPool 会创建一个含有足够多线程的线程池，来维持相应的并行级别，他会通过工作窃取的方式，使得多核的 CPU 不会闲置，
总会有或者的 CPU 去运行。

工作窃取算法（work Stealing）: 闲置的线程去处理本不属于它的任务，本质上就是利用了 ForkJoinPool


### 原理

ThreadPoolExecutors 中使用了 ctl 用来表示线程池中的重要数据：利用低29位表示线程池中的线程数，用高3位表示线程池的状态：

1. RUNNING: 111，该线程池会接收新任务，并处理阻塞队列中的任务
2. SHUTDOWN: 000，该线程池不会接收新任务，但会处理阻塞队列中的任务
3. STOP: 001，该线程池不会接收新任务，也不会处理阻塞队列中的任务，且会中断正在运行中的任务
4. TIDYING: 010, 所有的任务都已经终止，workerCount 的数目为0，资源释放
5. TERMINATED: 011，线程池彻底终止

```
// use ctl indicate the state of executors and count of thread 
private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
private static final int COUNT_BITS = Integer.SIZE - 3;
private static final int CAPACITY   = (1 << COUNT_BITS) - 1;

// runState is stored in the high-order 3 bits
private static final int RUNNING    = -1 << COUNT_BITS;
private static final int SHUTDOWN   =  0 << COUNT_BITS;
private static final int STOP       =  1 << COUNT_BITS;
private static final int TIDYING    =  2 << COUNT_BITS;
private static final int TERMINATED =  3 << COUNT_BITS;

// high 3 bits means runState
private static int runStateOf(int c)     { return c & ~CAPACITY; }

// low bits means workerCount
private static int workerCountOf(int c)  { return c & CAPACITY; }
private static int ctlOf(int rs, int wc) { return rs | wc; }
```


#### execute()

1. `workerCountOf()` 根据ctl 的低29位，得到当前线程池中的线程数，如果线程池小于 corePoolSize，则执行 `addWorker()` 创建
新的线程执行任务
2. 如果线程池处于 RUNNING 状态，且能够成功得把任务提交到阻塞队列中，则执行步骤3，否则执行步骤4
3. 再次检查线程池的状态，如果线程池没有 RUNNING，且成功从阻塞队列中删除任务，则执行 reject() 处理任务
4. 执行 addWorker() 创建新的线程执行任务，如果 addWorker 失败，那么执行 reject() 处理任务

```
int c = ctl.get();

// if countWorker < corePoolSize, it need to addWorker
if (workerCountOf(c) < corePoolSize) {
    if (addWorker(command, true))
        return;
    c = ctl.get();
}

// if executors is running and offer task into queue successfully (worker start taking work to do), 
// after that recheck the executors state
if (isRunning(c) && workQueue.offer(command)) {
    int recheck = ctl.get();
    if (! isRunning(recheck) && remove(command))
        reject(command);
    else if (workerCountOf(recheck) == 0)
        addWorker(null, false);
}
else if (!addWorker(command, false))
    reject(command);
```

#### addWorker()

addWorker() 的上半部分主要是通过 for(;;) 循环判断线程池的 state 和 coreSize  
下半部分主要是将构建 Worker 实例，并加入到 HashSet 中，同时启动 Worker 中的线程。

```
private boolean addWorker(Runnable firstTask, boolean core) {
    ...
    boolean workerStarted = false;
    boolean workerAdded = false;
    Worker w = null;
    try {
        w = new Worker(firstTask);
        final Thread t = w.thread;
        if (t != null) {
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                // Recheck while holding lock.
                // Back out on ThreadFactory failure or if
                // shut down before lock acquired.
                int rs = runStateOf(ctl.get());

                if (rs < SHUTDOWN ||
                    (rs == SHUTDOWN && firstTask == null)) {
                    if (t.isAlive()) // precheck that t is startable
                        throw new IllegalThreadStateException();
                    workers.add(w);
                    int s = workers.size();
                    if (s > largestPoolSize)
                        largestPoolSize = s;
                    workerAdded = true;
                }
            } finally {
                mainLock.unlock();
            }

            // start thread, thread start to do firstTask or getTask()
            if (workerAdded) {
                t.start();
                workerStarted = true;
            }
        }
    } finally {
        if (! workerStarted)
            addWorkerFailed(w);
    }
    return workerStarted;
}
```

#### Worker

Worker 作为工作队列中的消费者，设计如下：

1. 继承 AbstractQueuedSynchronizer，可以方便的实现工作线程的中断操作
2. 实现 Runnable 接口，可以将自身作为一个任务在工作队列中执行
3. 当前提交的任务 firstTask 可作为参数传入 Worker 的构造方法

从 Worker 的构造方法实现可以发现：线程工厂在创建线程 thread 时，将 Worker 实例本身 this 作为参数传入，当执行 start()
启动线程 thread 时，本质上时执行了 worker 的 `runWorker()`。
```
private final class Worker
    extends AbstractQueuedSynchronizer
    implements Runnable{

    Worker(Runnable firstTask) {
        setState(-1); // inhibit interrupts until runWorker
        this.firstTask = firstTask;
        this.thread = getThreadFactory().newThread(this);
    }
}
```
 
#### runWorker

runWorker() 具体的任务执行过程：

1. 线程启动后，通过 unlock() 释放锁，设置 AQS 的state 为0，表示运行中断
2. 获取第一个任务 firstTask，执行任务中的 run()，在执行任务之前，会进行加锁操作，任务执行完会释放锁
3. 在任务执行的前后，可以根据业务场景自定义 `beforeExecute()` 和 `afterExecutor()`
4. firstTask 执行完成后，通过 `getTask()` 从阻塞队列中获取等待的任务，如果队列中没有任务，`getTask()` 会被阻塞并挂起，
不会占用 cpu 资源 

```
final void runWorker(Worker w) {
        Thread wt = Thread.currentThread();
        Runnable task = w.firstTask;
        w.firstTask = null;
        w.unlock(); // allow interrupts
        boolean completedAbruptly = true;
        try {
            while (task != null || (task = getTask()) != null) {
                w.lock();
                // If pool is stopping, ensure thread is interrupted;
                // if not, ensure thread is not interrupted.  This
                // requires a recheck in second case to deal with
                // shutdownNow race while clearing interrupt
                if ((runStateAtLeast(ctl.get(), STOP) ||
                     (Thread.interrupted() &&
                      runStateAtLeast(ctl.get(), STOP))) &&
                    !wt.isInterrupted())
                    wt.interrupt();
                try {
                    beforeExecute(wt, task);
                    Throwable thrown = null;
                    try {
                        task.run();
                    } catch (RuntimeException x) {
                        thrown = x; throw x;
                    } catch (Error x) {
                        thrown = x; throw x;
                    } catch (Throwable x) {
                        thrown = x; throw new Error(x);
                    } finally {
                        afterExecute(task, thrown);
                    }
                } finally {
                    task = null;
                    w.completedTasks++;
                    w.unlock();
                }
            }
            completedAbruptly = false;
        } finally {
            processWorkerExit(w, completedAbruptly);
        }
    }
```

#### getTask()

整个 getTask() 在自旋下完成（不要不断访问队列取任务）:

1. workQueue.take(): 如果阻塞队列为空，当前线程会被挂起等待；当队列中有任务加入时，线程被唤醒，take() 方法直接返回任务
2. workQueue.pool(): 如果在 keepAliveTime 时间内，阻塞队列还是没有任务，则返回null


```
private Runnable getTask() {
    boolean timedOut = false; // Did the last poll() time out?

    for (;;) {
        int c = ctl.get();
        int rs = runStateOf(c);

        // Check if queue empty only if necessary.
        if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
            decrementWorkerCount();
            return null;
        }

        int wc = workerCountOf(c);

        // Are workers subject to culling?
        boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;

        if ((wc > maximumPoolSize || (timed && timedOut))
            && (wc > 1 || workQueue.isEmpty())) {
            if (compareAndDecrementWorkerCount(c))
                return null;
            continue;
        }

        try {
            Runnable r = timed ?
                workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) :
                workQueue.take();
            if (r != null)
                return r;
            timedOut = true;
        } catch (InterruptedException retry) {
            timedOut = false;
        }
    }
}
```


### worker 的生命周期

1. worker 在一个 while 中不断从阻塞队列中取出任务
2. keepAliveTime 时间内获取不到任务，或则获取任务成功但执行失败，退出 while 循环，执行 `processWorkerExit()`
3. 如果结束该 worker 之后，需要补充 worker，则重新调用 `addWorker()` 重新补充一个，否则，一个worker 的生命结束，
等待被 GC （从 workersSet 被移除之后，这个 worker 再没有任何引用，将会被 GC）
 
### 引用
[深入分析java线程池的实现原理](https://www.jianshu.com/p/87bff5cc8d8c)  