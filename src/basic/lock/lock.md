### synchronized

1. 同步代码块中使用了 monitorenter 和 monitorexit 指令实现
2. 同步方法中依靠方法修饰符上的 ACC_SYNCHRONIZED 实现

无论是同步块，同步方法都是对指定对象相关联的 monitor 的获取，这个过程是互斥的，即同一时刻只能有一个线程能够成功，
其它失败的线程会被阻塞，并放入到同步队列中，进入 BLOCKED 状态。

#### Java 对象头

对象头：Mark Word(hashCode、gc age、lock record...) + Class Metadata Address 存储到对象类型数据的指针

当对象状态为偏向锁时，Mark Word 存储的是偏向的 `线程ID` ；当状态为轻量级锁时，Mark Word 存储的是指向线程栈中 
`Lock Record` 的指针；当状态为重量级锁时，Mark Word 为指向堆中的 `monitor` 对象的指针。

#### monitor

monitor 是线程私有的数据结构，每一个线程都有一个可用的 monitor 列表（为了同时锁住多个资源），同时还有一个全局可用列表，
内部结构如下：(以线程的角度试想 synchronized)

* Owner: 初始化时为 NULL 表示当前没有任何线程拥有该 monitor，当线程成功拥有该锁后，保存线程唯一标识，当锁释放时又设置为NULL
* EntryQ: 关联一个系统的互斥锁（semaphore），阻塞所有试图锁住 monitor 失败的线程
* RcThis：标识 blocked 或 waiting 在该 monitor 上的所有线程个数
* Nest: 用来实现重入锁的计数
* HashCode: 保存从对象头拷贝过来的 HashCode 值（可能包含 GC age）
* Candidate: 用来避免不必要的阻塞或等待线程唤醒，因为每一次只有一个线程能够成功拥有锁，如果每次前一个释放锁的线程唤醒
所有正在阻塞或等待的线程，会引起不必要的上下文切换（从阻塞到就绪然后因为竞争锁失败又被阻塞），从而导致性能下降。
Cadidate： 0表示没有需要唤醒的线程，1表示要唤醒一个继任线程在争夺锁

作用：当线程一旦进入到被 `synchronized` 修饰的方法或者代码块时，指定的锁对象将对象头中的 LockWord 指向 monitor 的起始地址
与之关联，同时 monitor 中 Owner 存放拥有该锁线程的唯一标识，确保一次只能有一个线程执行该部分的代码，线程在获取锁之前不允许
执行该部分的代码。（其他线程进来 -> 对象头 lockWord -> monitor -> Owner(持有的线程标识)）

### 偏向锁（同一个对象锁）

当线程访问 synchronized method1()，会在 `被锁的对象头` 和 `栈帧` 的锁记录中存储锁偏向的线程id，下次线程再次进入相同锁对象
的方法 synchronized method2() 时，只需要判断对象头存储的线程id 是否为当前线程，而不需要 cas 操作进行加锁和解锁（cas仍存在
本地延迟）

```
public static void main(String[] args) {
    // set main thread id on BiasedLock.class head and frame stack
    method1();
    // next time when main thread call method2(), check BiasedLock.class head, and continue 
    method2();
}
synchronized static void method1(){}
synchronized static void method2(){}
```

### 轻量级锁

线程通过两种方式锁住对象：

1. 通过膨胀一个处于无锁状态 (状态位001) 的对象获得该对象的锁
2. 对象处于膨胀状态（状态位00），但 LockWord 指向的 monitor 的 Owner 字段位 NULL，则可以直接通过 CAS 原子指令尝试将
Owner 设置为自己的标识来获得锁

获取锁(monitorenter) ：

1. 对象处于无锁状态时 （LockWord 的值为 hashCode，状态位为001等），线程先从 monitor 列表中取得一个空闲的 monitor，
初始化 Nest 和 Owner的值为1，一旦 monitor 准备好，通过 cas 替换 monitor 的起始地址到 lockWord 进行膨胀（lockWork -> monitor）。
如果存在其他线程竞争锁的情况导致 cas 失败，则重新 monitorenter 重新开始获取锁的过程。

2. 对象已经膨胀，monitor 中的 Owner 指向当前线程，这时重入锁的情况，将 Nest+1，不需要cas操作，效率高

3. 对象已经膨胀，monitor 中的 Owner 为 NULL，此时多个线程通过 CAS 指令试图将 Owner 设置为自己的标识获得锁。竞争失败的线程将
进入第4中情况。

4. 对象已经膨胀，同时 Owner 指向别的线程，在调用操作系统的重量级的 __互斥锁__ 之前自旋一定的次数，当达到一定的次数如果仍然
没有获得锁，则开始准备进入阻塞状态，将 RcThis + 1，由于在 +1 的过程中可能被其他线程破坏对象与 monitor 之间的联系，
所以在 +1 后需要再进行一次比较确保 lockWord 的值没有被改变，当发现被改变后则要重新进行 monitorenter 过程。同时再一次观察
Owner 是否为NULL，如果是则调用CAS参与竞争锁，锁竞争失败则进入到阻塞状态
 
释放锁(monitorexit)：

1. 检查该对象是否处于膨胀状态并且该线程是这个锁的拥有者，如果发现不对则抛出异常

2. 检查 Nest 字段是否大于 1，如果大于1则 Nest-1 并继续拥有锁，如果等于1，则进入到步骤3

3. 检查 RcThis 是否大于1，设置 Owner为 NULL，然后唤醒一个正在阻塞或等待的线程再一次试图获取锁，如果等于0则进入步骤4

4. 缩小（deflate）一个对象，通过该对象的 LockWord 置换回原来的 HashCode 等值来解除和 monitor 之间的关联来释放锁，
同时将monitor 放回线程私有的可用 monitor列表。

```
public static void main(String[] args) {
    Thread t1 = new Thread(new LightWeightLock(), "A");
    t1.start();

    Thread t2 = new Thread(new LightWeightLock(), "B");
    t2.start();
}

@Override
public void run() {
    method1();
    method2();
}

synchronized static void method1(){};
synchronized static void method2(){};
```

#### 重量级锁

当锁处于这个状态下，其他线程试图获取锁都会被阻塞住，当持有锁的线程释放锁之后回唤醒这个线程


### AbstractQueuedSynchronizer

同步队列中的节点状态有：

1. CANCELLED（1）: 取消状态, 表明前置节点已经等待超时或已经被中断，需要从等待列表中删除
2. SIGNAL（-1）: 等待触发状态，表明当前节点需要阻塞
3. CONDITION（-2）: 等待条件状态，表明当前节点在等待 condition， 即在 condition 队列中
4. PROPAGATE（-3）: 状态需要向后传播，表示 releaseShared 需要被传播给后续节点，仅在共享锁模式下

#### 线程获取锁的过程：

1. 线程A 执行 cas 执行成功，state值被修改并且返回true，线程A继续执行
2. 线程A 执行 cas 失败，说明线程B 在执行cas 且成功，这种情况下线程A继续执行步骤3
3. 生成新的节点 node，并通过 cas 插入到等待队列的队尾（同一时刻可能会有多个 node 插入到等待队列中），如果 tail 为空，
则将 head 节点指向一个空节点（代表线程B），实现如下：

```
// add new node to wait queue
private Node addWaiter(Node mode) {
    Node node = new Node(Thread.currentThread(), mode);

    // Try the fast path of enq; backup to full enq on failure
    Node pred = tail;
    if (pred != null) {
        node.prev = pred;

        // try to cas enqueue once, if succee, return directly
        if (compareAndSetTail(pred, node)) {
            pred.next = node;
            return node;
        }
    }
    // if try to  cas enqueue failed, loop cas enqueue
    enq(node);
    return node;
}

// loop enqueue
private Node enq(final Node node) {
    for (;;) {
        Node t = tail;
        if (t == null) { // Must initialize
            if (compareAndSetHead(new Node()))
                tail = head;
        } else {
            node.prev = t;
            if (compareAndSetTail(t, node)) {
                t.next = node;
                return t;
            }
        }
    }
}
```

4. node 插入到队尾后，该线程并不会立马挂起，会进行自旋操作。因为在 node 插入过程，线程B (之前没有阻塞的线程) 可能已经
执行完成，所以要判断该 node 的前一个节点 pred 是否为 head 节点（代表线程B），如果 pred == head，表明当前节点是队列中
第一个 “有效的” 节点，因此将再次尝试 `tryAcquire()` 获取锁。
    1. 如果成功获取到锁，表明线程B 已经完成执行，线程A 不需要挂起。
    2. 如果获取失败，表明线程B 还未完成，至少还未修改 state值，进行步骤5

```
// try acquire lock before park
final boolean acquireQueued(final Node node, int arg) {
    boolean failed = true;
    try {
        boolean interrupted = false;
        for (;;) {
            final Node p = node.predecessor();

            // if pred is head and hold lock success, then return
            if (p == head && tryAcquire(arg)) {
                setHead(node);
                p.next = null; // help GC
                failed = false;
                return interrupted;
            }

            // check pred node state and try to interruot thread 
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                interrupted = true;
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}
```

5. 因为只有当前一个节点 pred 的线程状态为 SIGNAL 时，当前节点的线程才能被挂起。
    1. 如果 pred 的 waitStatus == 0，则通过 cas 修改 waitStatus 为 Node.SIGNAL。
    2. 如果 pred 的 waitStatus > 0，表明 pred 的线程状态为 CANCELLED，需从列表中删除
    3. 如果 pred 的 waitStatus 为 Node.SIGNAL，则通过 `LockSupport.park()` 将线程挂起，并等待被唤醒，被唤醒后进入步骤6
    
```
private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
    int ws = pred.waitStatus;
    if (ws == Node.SIGNAL)
        /*
         * This node has already set status asking a release
         * to signal it, so it can safely park.
         */
        return true;
    if (ws > 0) {
        /*
         * Predecessor was cancelled. Skip over predecessors and
         * indicate retry.
         */
        do {
            node.prev = pred = pred.prev;
        } while (pred.waitStatus > 0);
        pred.next = node;
    } else {
        /*
         * waitStatus must be 0 or PROPAGATE.  Indicate that we
         * need a signal, but don't park yet.  Caller will need to
         * retry to make sure it cannot acquire before parking.
         */
        compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
    }
    return false;
}
```

6. 线程每次被唤醒时，都要进行中断检测，如果发现当前线程被中断，那么抛出 InterruptedException 并退出循环。从 for 循环中
可以看出，并不是被唤醒的线程一定能获得锁，必须调用 `tryAcquire()` 重新竞争，因为锁是非公平的，可能被新加入的线程获得，
从而导致刚被唤醒的线程再次被阻塞，这个细节充分提现了 __“非公平”__。

```
if (shouldParkAfterFailedAcquire(p, node) &&
      parkAndCheckInterrupt())
        throw new InterruptedException();
```

#### 线程释放锁的过程

1. 如果头节点 head 的 waitStatus 为-1，则通过 cas 重置为0，清除头节点的状态
2. 从队列尾部向前找到 waitStatus < 0 可唤醒的节点 s，通过 `LockSupport.unpark()` 唤醒线程

```
private void unparkSuccessor(Node node) {
    /*
     * If status is negative (i.e., possibly needing signal) try
     * to clear in anticipation of signalling.  It is OK if this
     * fails or if status is changed by waiting thread.
     */
    int ws = node.waitStatus;
    if (ws < 0)
        compareAndSetWaitStatus(node, ws, 0);

    /*
     * Thread to unpark is held in successor, which is normally
     * just the next node.  But if cancelled or apparently null,
     * traverse backwards from tail to find the actual
     * non-cancelled successor.
     */
    Node s = node.next;
    if (s == null || s.waitStatus > 0) {
        s = null;
        for (Node t = tail; t != null && t != node; t = t.prev)
            if (t.waitStatus <= 0)
                s = t;
    }
    if (s != null)
        LockSupport.unpark(s.thread);
}
```


### ReentrantLock

```
public class ReentrantLock implements Lock{
    private final Sync sync;

    abstract static class Sync extends AbstractQueuedSynchronizer {
        
        // try acquire
        final boolean nonfairTryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                if (compareAndSetState(0, acquires)) {

                    // set current be the exclusiveOwnerThread
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }

            // reentrant lock, state means the reentry times
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0) // overflow
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
        }

        // try release
        protected final boolean tryRelease(int releases) {
            int c = getState() - releases;
            if (Thread.currentThread() != getExclusiveOwnerThread())
                throw new IllegalMonitorStateException();
            boolean free = false;
            if (c == 0) {
                free = true;
                setExclusiveOwnerThread(null);
            }
            setState(c);
            return free;
        }
    }
}
```

#### noFairLock

1. 线程A 和线程B 同时执行 cas 指令，假设 A 执行成功，线程B 失败，则表明线程A 获得了锁，并把同步器中的 exclusiveOwnerThread
设置为线程A
2. 竞争失败的线程B，在 `nofairTryAcquire()` 中会再次尝试获取锁，如果失败将入队（AQS 的基本流程）

```
// default nofair acquire lock
static final class NonfairSync extends Sync {
    final void lock() {
        if (compareAndSetState(0, 1))
            setExclusiveOwnerThread(Thread.currentThread());
        else
            acquire(1);
    }
    
    protected final boolean tryAcquire(int acquires) {
        return nonfairTryAcquire(acquires);
    }
}
```

![](/src/img/nofairlock.webp)


#### fairLock

在公平锁中，与非公平锁不同的一点是在 `tryAcquire()` 的时候会调用 `hasQueuedPredcessors()`，目的是查找比当前线程更早的线程，
这样就保证了等待时间最长的会被最先唤醒获得锁。

```
static final class FairSync extends Sync {
     protected final boolean tryAcquire(int acquires) {
            ...
            // hasQueuedPredecessors()
            if (!hasQueuedPredecessors() &&
                compareAndSetState(0, acquires)) {
                setExclusiveOwnerThread(current);
                return true;
            }
            ...
     }
}

public final boolean hasQueuedPredecessors() {
    Node t = tail; // Read fields in reverse initialization order
        Node h = head;
        Node s;
        return h != t &&
            ((s = h.next) == null || s.thread != Thread.currentThread());
}
```


### 共享锁

共享锁与独占锁的区别在于，独占锁是独占的，排他的，因此在独占锁中有 exclusiveOwnerThread 属性，用于记录当前持有锁的线程。
当独占锁被某个线程占有时，其他线程只能等待它被释放后，才能取争夺锁，并且同一时刻只能有一个线程争夺成功。(联想 tryAcquire()
失败后，acquireQueued())。

对于共享锁而言，锁是共享的，所以能被多个线程同时持有。因此一个线程如果成功得获取了锁，那么其他等待在这个共享锁上的线程也可以
继续获得锁。

#### 实现

在独占锁模式中，我们只有在获取了独占锁的节点释放锁时，才会唤醒后继节点。因为独占锁只能被一个线程持有，如果还没被释放，
那么没必要唤醒后续接待你。

然而，在共享模式下，当一个节点获取到了共享锁，我们在获取成功后可以唤醒后续节点，而不需要等待该节点释放锁的时候，
因为共享锁可以被多个线程同时拥有，一个锁取到了，则后续的节点都可以直接来获取。因此，在共享锁模式下，在获取锁和释放锁
结束后，都会唤醒后继节点。

* 独占锁的实现

和上面介绍的一样，获取锁的过程中，先 `tryAcquire(arg)` （具体实现由场景决定）判断是否能获取锁，如果失败将执行 `acquireQueued()`
将线程加入到等待队列中。

```
public final void acquire(int arg) {
    if (!tryAcquire(arg) &&
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        selfInterrupt();
}
```

* 共享锁实现

和独占锁一样，先 `tryAcquireShared(arg)` (具体实现由场景决定) 尝试获取锁，如果获取失败，则调用 `doAcquireShared()` 
进行入队操作，同时唤醒后续线程。

```
public final void acquireShared(int arg) {
    if (tryAcquireShared(arg) < 0)
        doAcquireShared(arg);
}
```

和独占锁的过程差不多，显示调用 `addWaiter(Node.SHARED)` 将线程加入等待队列，然后自旋一段时间判断能否获得锁，
如果获得成功的话，通过 `setHeadAndPropagate()` 唤醒后续节点去获取这个共享锁资源。

```
private void doAcquireShared(int arg) {
    final Node node = addWaiter(Node.SHARED);
    boolean failed = true;
    try {
        boolean interrupted = false;
        for (;;) {
            final Node p = node.predecessor();
            if (p == head) {
                int r = tryAcquireShared(arg);
                if (r >= 0) {
                    setHeadAndPropagate(node, r);
                    p.next = null; // help GC
                    if (interrupted)
                        selfInterrupt();
                    failed = false;
                    return;
                }
            }
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                interrupted = true;
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}
```


#### setHeadAndPropagate()

1. 将当前节点设置成新的头节点，这意味着前置节点（旧头节点）已经获得共享锁，并从队列中移除了(这点和独占锁一样)
2. 调用 `doReleaseShared()`，它会调用 `unparkSuccssor()` 唤醒后继节点

```
private void setHeadAndPropagate(Node node, int propagate) {
    // set new queue head
    Node h = head; // Record old head for check below
    setHead(node);

    if (propagate > 0 || h == null || h.waitStatus < 0 ||
        (h = head) == null || h.waitStatus < 0) {
        Node s = node.next;

        // // if new head node's next node is  null or shared, signal next node
        if (s == null || s.isShared())
            doReleaseShared();
    }
}
```

#### doReleaseShared

释放锁的时候和独占锁有些不同：

* 线程不同：

在独占锁中，只有获取了锁的线程才能调用 release 释放锁，然后调用 `unparkSuccessor()` 唤醒后续的节点持有的线程。

在共享锁中，持有共享锁的线程会同时存在多个，所以在 `acquireShared()` 和 `releaseShared()` 释放锁，目的都是为了唤醒 head
节点的下一个节点，这点和独占锁相似，但在共享锁中，当头节点发生变化时，会回到循环中立即唤醒 head 的下一个节点。

```
private void doReleaseShared() {
    for (;;) {
        Node h = head;
        if (h != null && h != tail) {
            int ws = h.waitStatus;

            // if head status == SIGNAL, it means it need to be signal, if failed, loop try again
            if (ws == Node.SIGNAL) {
                if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                    continue;            // loop to recheck cases
                unparkSuccessor(h);
            }
            
            // if next node don't need to signal, set the head status = PROPAGATE, 
            // make sure it can pass the status to next after that.
            else if (ws == 0 &&
                     !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                continue;                // loop on failed CAS
        }
        if (h == head)                   // loop if head changed
            break;
    }
}
```

因为多个线程会同时执行加锁和释放操作，而只有在 `doAcquireShared()` 的时候才会更新头节点的head 信息，假如队列 A->B->C，
当 A 释放锁后，唤醒B，这时B为节点即 B->C，在唤醒B后，B线程也会开始唤醒下一个节点C，在唤醒期间，A在执行h == head时，发现
自己已经不是头节点了，那么回到循环中，帮助协助完成唤醒操作。

通过for循环，判断头节点的状态：
1. 如果头节点head 的状态为 SIGNAL，说明头节点可以唤醒了，那么采用cas的方式更改节点状态为0，并唤醒它
2. 如果头节点head 的状态为0，说明不需要唤醒，那么 cas 设置状态为 PROPAGATE，确保下次状态传播


#### 应用

独占锁表示某个线程持续占有资源，我们可以很容易想到锁，对应的实现就是 ReentrantLock.sync，同时还有 ThreadPoolExecutor.worker。

共享锁可用于表示控制多个线程占有资源，例如 CountDownLatch, Semaphore, ReentrantReadWriteLock


### Condition

Condition 是 ReentrantLock 拓展 wait/notify 而引用的新的阻塞唤醒机制。

#### wait/notify 的不足

在 synchronized 中，所有调用 wait() 的线程，都会在同一个监视器锁 wait set 中等待，但每一个等待的线程可能等待在不同的条件
上，有时即使自己等待的条件并没有被满足，线程仍然能被其他线程调用 notify() 唤醒，因为大家都是使用同一个监视器锁，这样就会
造成即使自己被唤醒后，抢到了监视器锁，但仍然发现条件还是不满足，还是得调用 wait() 挂起，导致了很多无意义的时间和 CPU 资源
的浪费。  
而有了 Condition 后，我们就可以在同一个锁上创建不同的唤醒条件，从而在一定的条件满足后，针对性的唤醒特定的线程，
而不是直接唤醒所有线程。


#### 实现

每构建一个 Condition 对象，对应着一个 condition queue，每个 condition queue 都是互相独立的。当条件满足后，调用条件队列的
`signal()`，会将所有等待在这个条件队列中的线程唤醒，被唤醒的线程和普通线程一样需要争夺锁，如果没有抢到，
则同样要被加入到 sync queue 中。

#### sync queue & condition queue 入队出队状态

* sync queue: 入队时没有锁，在队列中争夺锁，离开队列时获取了锁
* condition queue: 入队时获取了锁，在队列中释放了锁，离开队列时没有锁，转移到 sync queue 

### 引用

[深入浅出synchronized](https://www.jianshu.com/p/19f861ab749e)  
[深入浅出java同步器AQS](https://www.jianshu.com/p/d8eeb31bee5c)  
[逐行分析AQS源码(4)——Condition接口实现](https://segmentfault.com/a/1190000016462281)  


