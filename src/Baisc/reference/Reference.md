
## GC 回收的问题

1. 对象因为被 Finalizer 引用从而变成一个临时的强引用，因此无法立即被回收
2. 对象至少经历两次 GC 才能被释放，因为只有在 FinalizerThread 执行完 f对象的 finalize() 的情况下，才有可能被 GC 回收，
   而这个期间可能经历了多次 GC，但是一直没有执行对象的 finalize()
3. CPU 资源比较稀缺的情况下，FinalizerThread 有可能因为优先级比较低而延迟执行对象的 finalize()
4. 因为对象的 finalize() 迟迟没有进行执行，有可能导致大部分 f() 对象进入到 old分代，此时容易引发 old分代的 GC，甚至 Full GC，
    GC暂停的时间明显变长，甚至 OOM
5. f对象的 finalize() 被调用后，可能因为 finalize() 中的 执行任务比较大，导致 finalize() 时间场，f对象就迟迟无法回收


## 使用 finalizer / clean 的好处？

1. 虽然 finalize / cleaner 并不会保证立即运行，但她能保证最终的资源释放，所以在 FileInputStream, ThreadPoolExecutor 都在 finalize()中
    对资源进行了释放操作
    
2. 合理得使用 Cleaner 机制得方法与本地对等类 (native peers)有关，本地对等类是一个由普通对象委托得本地（非Java）对象。由于不是Java对象，那么
    垃圾回收器并不会回收它，当Java对象被回收时，本地对等类也不会回收。如果没有关键资源，可由 Finalizer / Cleaner 机制进行回收 


##Reference 
    
    -> SoftReference, WeakReference, PhantomReference

内部提供两个构造函数：

```
    Reference(T referent) {
        this(referent, null);
    }
    
    // 携带 queue 的意义在于，我们通过 queue 对引用的对象进行监控，当对象被回收的时候，对应的 reference 对象将被放入到 queue 中
    // 如果没有指定 queue的话，就需要用户自己轮询 reference.get()，判断是否返回 null
    Reference(T referent, ReferenceQueue<? super T> queue) {
        this.referent = referent;
        this.queue = (queue == null) ? ReferenceQueue.NULL : queue;
    }
```

容易混淆的一点：为什么 Reference 不会被回收，而是 reference关联的对象？  

因为 Reference 的创建时通过 new 出来的，本身为强引用，即引用对象本身也是对象，就需要我们自己处理  
eg: Reference<Object> ref = new SoftReference(obj, queue);


### 进入 queue 中的 reference 引用的 obj status?

SoftReference: 当对象进入到 queue 的时候，说明当前内存不足，触发gc()，这时 obj = null  
WeakReference: 当对象进入时，说明gc()，这是 obj = null  
PhantomReference: 因为 phantomRef 本身的 get() 就是返回 null，所以 null
FinalReference: 因为需要调用对象的 finalize(), 即使进入 queue, obj也不会为null，除非已经完成