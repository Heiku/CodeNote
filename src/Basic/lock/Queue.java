package Basic.lock;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author: Heiku
 * @Date: 2019/12/12
 */
public class Queue<T> {
    private final T[] items;
    private final Lock lock = new ReentrantLock();

    private Condition notFull = lock.newCondition();
    private Condition notEmpty = lock.newCondition();

    private int head, tail, count;

    public Queue(){
        this(10);
    }

    public Queue(int maxSize){
        items = (T[]) new Object[maxSize];
    }

    public void put(T t) throws InterruptedException{
        lock.lock();

        try {
            while (count == items.length){
                // array is full, block all the put thread
                notFull.await();
            }
            // set value
            items[tail] = t;
            if (++tail == items.length){
                tail = 0;
            }

            ++count;
            // signal the consumer to take out element
            notEmpty.signal();
        }finally {
            lock.unlock();
        }
    }


    public T take() throws InterruptedException{
        lock.lock();
        try {
            while (count == 0){
                // array is empty, block all the take thread
                notEmpty.await();
            }
            T o = items[head];
            if (++head == items.length){
                head = 0;
            }
            --count;
            notFull.signal();
            return o;
        }finally {
            lock.unlock();
        }
    }
}
