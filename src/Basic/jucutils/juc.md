### CountDownLatch

CountDownLatch(闭锁): 允许一个或多个线程等待其他线程完成操作后再执行。CountDownLatch 内部会维护一个初始值为线程数量的
计数器，主线程执行 `await()`，如果计数器大于0，则阻塞等待，当一个任务完成后，计数器减一。当计数器为0时，表示所有的线程
已经完成任务，等待的主线程被唤醒继续执行。  
[CountDownLatch](/src/Basic/jucutils/countdownlatch/CountDownLatchApplication.java)

### Sync

CountDownLatch 的实现是基于 AQS, 在初始化阶段，会将所需要阻塞的线程数 count 设置成 AQS 的状态值 state，每次 countDown()
都会将 state - 1。

```
private static final class Sync extends AbstractQueuedSynchronizer {

    Sync(int count) {
        setState(count);
    }

    int getCount() {
        return getState();
    }

    protected int tryAcquireShared(int acquires) {
        return (getState() == 0) ? 1 : -1;
    }

    protected boolean tryReleaseShared(int releases) {
        // Decrement count; signal when transition to zero
        for (;;) {
            int c = getState();
            if (c == 0)
                return false;
            int nextc = c-1;
            if (compareAndSetState(c, nextc))
                return nextc == 0;
        }
    }
}
```

#### await()

主线程通过执行 `await()`, 调用 `AQS.doAcquireSharedInterruptibly()`，采用的是共享锁模式，调用 `addWaiter()` 入队，再入队后
重试获取锁，如果成功则调用 `setHeadAndPropagate()` 唤醒后继节点，如果失败则通过 `LockSupport.park()` 阻塞主线程。

* 和独占锁一样，都是采用了入队后自旋的操作尝试获取锁，不同的是，一旦获取了共享锁，会调用 `setHeadAndPropagete()` 同时
唤醒后继节点，实现共享模式

```
private void doAcquireSharedInterruptibly(int arg)
    throws InterruptedException {

    // enqueue node in shared mode
    final Node node = addWaiter(Node.SHARED);
    boolean failed = true;
    try {
        for (;;) {
            final Node p = node.predecessor();
            if (p == head) {

                // before park, try to race shared lock, 
                int r = tryAcquireShared(arg);

                // if succcess, signal next node
                if (r >= 0) {
                    setHeadAndPropagate(node, r);
                    p.next = null; // help GC
                    failed = false;
                    return;
                }
            }

            // if failed, LockSupport.park block thread
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                throw new InterruptedException();
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

### doReleaseShared

通过for循环，判断头节点的状态：

1. 如果头节点head 的状态为 SIGNAL，说明头节点可以唤醒了，那么采用cas的方式更改节点状态为0，并唤醒它
2. 如果头节点head 的状态为0，说明不需要唤醒，那么 cas 设置状态为 PROPAGATE，确保下次状态传播

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



### CyclicBarrier

CyclicBarrier 也叫同步屏障，可以让一组线程达到一个屏障时被阻塞，知道最后一个线程达到屏障，所有被阻塞的线程才能继续执行。  
[CyclicBarrier](/src/Basic/jucutils/cyclicbarrier/Racce.java)

### await()

CyclicBarrier 的实现主要时基于 ReentrantLock，内部使用 Generation 控制屏障的循环使用。

1. 每当线程执行 `await()`，内部变量 count-1，如果 count != 0，说明有线程未到达屏障处，则在锁条件变量 trip 上等待
2. 当 count == 0，说明所有线程都已经到达屏障处，执行条件变量 `signalAll()` 唤醒等待中的线程


```
private int dowait(boolean timed, long nanos)
    throws InterruptedException, BrokenBarrierException,
           TimeoutException {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        final Generation g = generation;

        if (g.broken)
            throw new BrokenBarrierException();

        if (Thread.interrupted()) {
            breakBarrier();
            throw new InterruptedException();
        }

        int index = --count;
        if (index == 0) {  // tripped
            boolean ranAction = false;
            try {
                final Runnable command = barrierCommand;
                if (command != null)
                    command.run();
                ranAction = true;
                nextGeneration();
                return 0;
            } finally {
                if (!ranAction)
                    breakBarrier();
            }
        }

        // loop until tripped, broken, interrupted, or timed out
        for (;;) {
            try {
                if (!timed)
                    trip.await();
                else if (nanos > 0L)
                    nanos = trip.awaitNanos(nanos);
            } catch (InterruptedException ie) {
                if (g == generation && ! g.broken) {
                    breakBarrier();
                    throw ie;
                } else {
                    // We're about to finish waiting even if we had not
                    // been interrupted, so this interrupt is deemed to
                    // "belong" to subsequent execution.
                    Thread.currentThread().interrupt();
                }
            }

            if (g.broken)
                throw new BrokenBarrierException();

            if (g != generation)
                return index;

            if (timed && nanos <= 0L) {
                breakBarrier();
                throw new TimeoutException();
            }
        }
    } finally {
        lock.unlock();
    }
}
```

### Semaphore

Semaphore 也叫信号量，用来控制同时访问特定线程的数量，通过协调各个线程，以保证合理的使用资源。
内部基于 AQS，可用于做流量控制




### 引用
[深入浅出java CountDownLatch](https://www.jianshu.com/p/1716ce690637)