
### HashMap

#### 为什么 HashMap 的长度要保持在 2 的整数幂?  

因为这样, hash() & len - 1 的时候，才能构造出 0000 1111 类似这样的低位掩码  
类似于 0010 0101 & 0000 1111  
0101    
这样就将散列值的高位清零，只保留低位的值，用于访问数组下标  

#### 为什么 hash() 能减少发生冲突的概率？

因为 HashMap 在 1.8 里加入了 扰动函数 -> hash()， h = hashcode(key) ^ (h >>> 16)  
意思就是说将 hashcode 的 左16 与 右16 进行异或，充分混乱 hash 码的高位与低位，以此增加了低位的随机性

```
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
```

tableSizeFor 目的是找到 capacity 接近于 2^n 的数值，通过多个位右移，使低位都为1.
0100 0000   >>>1  0010 0000  |=  0110 0000  
0110 0000   >>>2  0001 1000  |=  0111 1000  
使最高位上的1后面的位全为1，最后 +1 得到我们想要的 2的整数次幂的值

```
static final int tableSizeFor(int cap) {
    int n = cap - 1;
    n |= n >>> 1;
    n |= n >>> 2;
    n |= n >>> 4;
    n |= n >>> 8;
    n |= n >>> 16;
    return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
}
```

#### 为什么 HashMap 不安全？

 * 首先，多线程在插入数据时，进行resize扩容的过程中，每个线程都有机会进入到方法中进行扩容操作，而在1.7中，因为
扩容后的链表会倒叙，所以但线程切换时，有可能会出现上一个线程持有数组下标节点，而下一个线程已经修改扩容完了，
所以上一个线程在持有节点的时候，不知道已经扩容完，等到其扩容时，节点间形成环，导致cpu 100%

造成死循环的原因主要在下面的代码：
```
void transfer(Entry[] newTable, boolean rehash) {
    for (Entry<K,V> e : table) {
        while(null != e) {
            Entry<K,V> next = e.next;
            int i = indexFor(e.hash, newCapacity);
            e.next = newTable[i];
            newTable[i] = e;
            e = next;
        }
    }
}
```

1.7的扩容思路是，遍历oldTable 中的 entry, 重新计算 hash 后，定位到 newTable[i] 中，每次遍历都会取到该位置下的首节点 head，
将 head 作为 当前节点的 next，这样就完成了新节点的插入操作。但这样的扩容会因为在倒序插入这样的特点下，形成闭环。

![](/img/hashmap1.7.webp)

想象一下，原来的数组 oldTable[3] a -> b -> c，  
然后进行扩容，多个线程在争夺，假如线程 A 先完成扩容（线程B 并不知情），这时在 newTable[7] c -> b -> a，倒序  
这时切换到了B 执行，先从a 开始，正常 newTable[7] a，  
然后开始插入 oldTable 中的 b，正常 newTable[7] b -> a，因为在线程A 中因为 b -> a，所以继续插入a，  
那么就会到时 a.next = newTable[7] b，即 a.next = b，a -> b, b -> a，就形成了闭环。

* 其次，最主要的还是日常操作中的put(),get(),size(),contain()等方法中，没有使用锁，导致多线程中数据替换丢失、
数据访问不一致的问题


### ConcurrentHashMap

1.7 分段锁，segment extends ReentrantLock  (Segment[] + HashEntry[])  
1.8 采用CAS + Synchronized  

* tabAt(): (Node<K,V>)U.getObjectVolatile(tab, ((long)i << ASHIFT) + ABASE);  

指向链表数组的引用table 本身是volatile，但链表数组中保存的每个元素并不是 volatile，Unsafe 的 getObjectVolatile() 保证
了每次从table 中读取某个位置链表引用的时候都是从主内存中读取的，如果不用该方法，有可能读取的是缓存中已有

* put(): 首先K V都不允许为空，否则报空指针异常，获取hash  
接着，开始遍历数组桶，如果table为空，则采用cas  initTable的方式初始化  
然后，通过hash值去定位数组桶，casTabAt()，得到头节点，注意这里 hash = -1 ：正在扩容，ForwardingNode节点扩容, hash = -2 ：树节点  
`synchronized` 的方式锁住头节点，然后链表替换、添加，或者树节点的添加替换  
最后还得判断长度是否大于8，转成树，还得记录对应的修改次数，容量加一

* get(): 为什么get()不需要加锁，就可以得到最新的值，而不会脏读？  
因为节点数组是通过 volatile修饰的，这里得注意，volatile修饰数组的时候，并不会对数组中的每一个元素具有可见性，
而是保证整个数组地址的可见。

如果发现当前位置是 MOVE 状态，说明正在扩容中，当前位置 node 属于 ForwardingNode，因为 forwardingNode(nextTable) 是在
nextTable 上，所以直接通过 forwardingNode.find() 直接找到对应得节点数据。

* size()：添加 addCount()：添加的两个对象分别是 baseCount + CounterCell[]，都使用volatile进行修饰  
一般来说采用 cas的方式增长 baseCount，如果失败的话，那么就采用 cas的方式添加到 CounterCell[]中，  
如果还是失败，采用for(;;)死循环添加，在添加的过程可以 通过参数 check去检查扩容  

统计长度的话，就可以采用 baseCount + for()遍历 CounterCeil[]的方式  累加

#### 扩容分析

ConcurrentHashMap在扩容上与1.7的时候差别很大，主要是引入了多线程扩容的概念，而不是锁住单独扩容

* helpTransfer():

这里引入一个 SizeCtl的变量概念，-1：table正在初始化，-N：表示N-1个线程正在扩容， >0：初始化完成,  
同时高低位分别保存着不同的概念： sizeCtl(高16：length标识符， 低16：并发扩容的线程数)  

主要过程是循环判断对sizeCtl的判断或者修改，通过sizeCtl 判断(标识符修改？扩容结束？达到最大扩容线程数？)  
否则，通过cas sizeCtl + 1，进行transfer

* transfer()：

首先，先确定为每个CPU分配对应的桶大小，默认16，保证每个CPU能处理一样多的桶
接着，创建一个两倍长度的 Node[]，初始化ForwardingNode(newTab)， 处理完槽点位的节点后，表示该槽位处理过了  
然后，for(int i = 0，bound = 0;;) 去遍历槽位确定对应的链表头。  
while（advance） 为每个线程去确定它所需要处理的槽区间，当处理完后，可以去剩余空间领取槽位空间（A | B | C | 最先处理完的线程领取这个剩余区间）
 
在处理前，判断tab == null ？ cas修改为fwd节点，如果hash == -1，表示其它线程在修改了，advance = true，跳过  
 
synchronized(f),对头节点进行加锁，创建高低节点 lowNode、highNode， runBit = fh & n  0 | 1，接着遍历，每个node的hash & n == runBit，区分高低节点  
最后再次遍历链表，将链表中的节点按照位数分成两条链表，获得的两条链表方向相反，casTabAt 放入数组的高低位中，设置forwardingNode，advance = true
 
如果槽位节点是红黑树节点，还是根据高低位lo，hi，树节点转换成两条链表，接着再将链表重新构造红黑树
 
### HashSet

内部就是一个map，只是将所有的值当作 map 中的 key，存放到map 中，如果map 中已经存在该(key, object)，
将会替换key，但key，object 都是一样的。同时map 会返回旧值（PERSENT），所以add 返回 false。

```
private static final Object PRESENT = new Object();

public boolean add(E e) {
    return map.put(e, PRESENT)==null;
}

public boolean contains(Object o) {
    return map.containsKey(o);
}
```

### LinkedHashMap

LinkedHashMap 有序的原因在于对每个 Node（Entry）维护了 before, after 两个对象引用，在插入数据的时候同时维护着这两个指针，
同时 `extends HashMap`，底层的存储还是以 HashMap 的数据 + 链表的形式。

```
    // 外部维护 head
    transient LinkedHashMap.Entry<K,V> head;

    // 外部维护 tail
    transient LinkedHashMap.Entry<K,V> tail;

static class Entry<K,V> extends HashMap.Node<K,V> {
     Entry<K,V> before, after;

     Entry(int hash, K key, V value, Node<K,V> next) {
     super(hash, key, value, next);
    }
}
```


### 排序

`Collections.sort()` 会将集合转化成数组arr，并调用 `Arrays.sort()` 对数组进行排序。

如果只是纯数组，会使用 `DualPivotQuicksort.sort()` 双端快排，避免了某些极端情况下，普通快排退化成 O(n^n)
如果指定了 Comparator，那么会使用 `TimSort.sort()` 进行排序

* TimSort

现实中的大多数数据通常是有部分已经排序好的数据块，TimSort 称这些已经排序好的数据块们为 "Natural runs"，
我们可以将其视为一个一个的 "分区"。在排序时，TimSort 迭代数据元素，将其放到不同的 run 里，同时针对这些 run，
按规则进行合并至只剩一个，则这个仅剩的 run 即为排好序的结果。

也就是说，TimSort 的大致思想是先采用 __插入排序__ 将非常小的 run 扩充为大的 run，然后采用 __归并排序__ 来合并多个 run，
所以 TimSort 实际为 __归并排序__。具体来说，我们需要定义一个参数 minrun，当 run 长度小于 minrun时，我们认为他是非常
小的 run，否则认为他是较大的 run。


```
1. 扫描数组，确定其中的单调上升段和严格单调下降段，将严格下降段反转

2. 定义最小片段长度 minrun，短于此的单调片段通过插入排序集中为长于此的片段

3. 反复归并一些相邻片段，过程中避免归并长度相差很大的片段，直至整个排序完成，所以分段选择策略可以保证 O(n logn)
```

### 引用

[HashMap? ConcurrentHashMap?](https://crossoverjie.top/2018/07/23/java-senior/ConcurrentHashMap/)  
[深入浅出ConcurrentHashMap1.8](https://www.jianshu.com/p/c0642afe03e0)  
[深入分析ConcurrentHashMap1.8的扩容实现](https://www.jianshu.com/p/f6730d5784ad)  
[谈谈ConcurrentHashMap1.7和1.8的不同实现](https://www.jianshu.com/p/e694f1e868ec)  