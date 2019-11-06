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