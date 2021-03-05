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

主线程通过执行 `await()`, 接着会调用 `AQS.acquireSharedInterruptibly()`，判断线程中断，接着调用 `tryAcquireShared()`，
这个方法会回调子类中的实现，而 `CountDownLatch.Syn` 中的实现逻辑是当 state != 0 的时候，将该线程加入到同步队列中，
等待唤醒。如果 state == 0，说明countDownLatch 已经没有需要阻塞的线程了，不需要操作。 

```
public void await() throws InterruptedException {
    sync.acquireSharedInterruptibly(1);
}

public final void acquireSharedInterruptibly(int arg)
        throws InterruptedException {
    if (Thread.interrupted())
        throw new InterruptedException();
    if (tryAcquireShared(arg) < 0)
        doAcquireSharedInterruptibly(arg);
}

protected int tryAcquireShared(int acquires) {
    return (getState() == 0) ? 1 : -1;
}
```

#### countDown()

每当其他线程执行 countDown() 时，都会将 state - 1，在 `tryReleaseShared()` 中，判断是否唤醒线程的根据是
判断当前的 state == 0，如果state == 0，说明已经没有线程需要等到了，阻塞的线程将在同步队列中按照共享锁的
方式被唤醒。

```
public void countDown() {
    sync.releaseShared(1);
}

public final boolean releaseShared(int arg) {
    if (tryReleaseShared(arg)) {
        doReleaseShared();
        return true;
    }
    return false;
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
内部基于 AQS，(和 CountDownLatch 一样，都是重写了 `tryAcquireShared()` & `tryReleaseShared()`)。

内部维护了一组虚拟许可证，许可的数量可以通过构造函数的参数指定。

```
abstract static class Sync extends AbstractQueuedSynchronizer {

    Sync(int permits) {
        setState(permits);
    }

    final int getPermits() {
        return getState();
    }

    final int nonfairTryAcquireShared(int acquires) {
        for (;;) {
            int available = getState();
            int remaining = available - acquires;
            if (remaining < 0 ||
                compareAndSetState(available, remaining))
                return remaining;
        }
    }

    protected final boolean tryReleaseShared(int releases) {
        for (;;) {
            int current = getState();
            int next = current + releases;
            if (next < current) // overflow
                throw new Error("Maximum permit count exceeded");
            if (compareAndSetState(current, next))
                return true;
        }
    }

    final void reducePermits(int reductions) {
        for (;;) {
            int current = getState();
            int next = current - reductions;
            if (next > current) // underflow
                throw new Error("Permit count underflow");
            if (compareAndSetState(current, next))
                return;
        }
    }

    final int drainPermits() {
        for (;;) {
            int current = getState();
            if (current == 0 || compareAndSetState(current, 0))
                return current;
        }
    }
}
```

* 在访问特定资源之前，必须使用 `acquire()` 获得方法许可，如果许可的数量为0，该线程将一直阻塞，直到有可用许可
1. acquires 表示请求的线程数，默认为1，remaining 表示剩余的许可数，如果 remaining < 0，表示目前没有剩余的许可数
2. 当前线程进入 AQS 中的 `doAcquireSharedInterruptibly()` 等待可用许可并挂起，直到被唤醒。

```
final int nonfairTryAcquireShared(int acquires) {
    for (;;) {
        int available = getState();
        int remaining = available - acquires;
        if (remaining < 0 ||
            compareAndSetState(available, remaining))
            return remaining;
    }
}
```

* 访问资源后，使用 `release()` 释放许可
1. 通过 `unsafe.compareAndSwapInt()` 修改 state 的值，确保同一时刻内只有一个线程可以释放成功
2. 许可释放成功后，当前线程进入到 `AQS.doReleaseShared()`，唤醒队列中的等待许可队列 

```
protected final boolean tryReleaseShared(int releases) {
    for (;;) {
        int current = getState();
        int next = current + releases;
        if (next < current) // overflow
            throw new Error("Maximum permit count exceeded");
        if (compareAndSetState(current, next))
            return true;
    }
}
```
### 引用
[深入浅出java CountDownLatch](https://www.jianshu.com/p/1716ce690637)  
[深入浅出java CyclicBarrier](https://www.jianshu.com/p/424374d71b67)  
[深入浅出java Semaphore](https://www.jianshu.com/p/0090341c6b80)  
