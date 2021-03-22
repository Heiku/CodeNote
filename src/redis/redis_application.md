# Redis Application

## 基础数据结构

### string

Redis 字符串采用的是动态字符串（sds），可以修改的字符串。内部结构实现上类似于 Java 中的 ArrayList，采用预分配冗余空间的方式 减少内存的频繁分配。（键值对、计数）

### list

Redis 列表相当于 Java 中的 linkedList(quickList/zipList)，所以在插入效率为 O(1)，但在删除查找为O(N)

实践：可以当作队列使用，rpush数据到 Redis，另一个客户端 lpop 取出数据进行消费。由于新消息写入后没有通知，这需要客户端 while(1) 
不断拉取数据，这会导致消费者程序一直消耗执行在 lpop 上，产生性能损失。可以用 `blpop` 阻塞式读取，节省 CPU 开销。

* 缺点：没有 ack 机制和不支持多个消费者，没有 ack 机制会导致从 Redis 取出数据后，如果客户端处理失败了（还没处理就宕机），取出的这个数据相当于丢失了， 
无法重新消费，所以 list 队列适合对于丢失数据不敏感的业务场景。同时，aof，主从切换过程中也会有丢失数据的风险。（依赖于内存，
如果出现消息堆积的情况，可能会 OOM）

* 优点：内存操作，快简单

### hash

类似于 Java 中的 HashMap，同样采用链表法存储碰撞的的元素。Redis 中的 rehash 采用的是渐进式的 rehash，rehash的同时， 保留新旧两个 hash 结构，查询同时查两个结构，知道迁移的线程完成任务才替代。

### Set

类似于 Java 中的 HashSet，字典中的所有 value 都是一个 NULL。

### ZSet

类似与 Java 中 SortedSet 和 HashMap 的结合。使用Set保证了 Value 的唯一性，使用map存储 value 对应的 score。 内部通过跳跃表实现。

### PubSub

Redis 提供的 PubSub，支持多个消费者进行消费，生产者每发布一条消息，多个消费者同时订阅消费。

* 缺点：

如果任意一个消费者挂了，等恢复过来，这期间的生产者数据就丢失了。PubSub 只是把数据发给在线的消费者，消费者一旦下线， 就会丢弃数据。另一个缺点是：PubSub 中的数据不支持数据持久化，如果 Redis 宕机，其他数据能通过 RDB
和 AOF 恢复过来，但 PubSub不行， 本身就是基于内存的简单多播机制

### Stream

Redis 5.0 之后推出了 Stream 数据结构，鉴于 Kafka，弥补了 List 和 PusSub 的不足，支持了 持久化、ack机制、多个消费者、数据回溯消费， 基本上实现了队列中间件的大部分功能，比 List 和 PubSub
更可靠。

* XADD: 插入消息，保证有序，可以自动生成全局唯一ID
* XREAD: 用于读取消息，可以按 ID 读取消息
* XREADGROUP: 按消费组形式读取消息
* XPENDING、XACK: XPENDING 命令可以用来查询消费组内所有消费者已读取但尚未确认的消息，而 XACK 用于向消息队列确认消息处理已完成


### HyperLogLog

pfadd/pfcount: 分别为增加计数、获取计数。 存在一定的误差 0.8%。底层使用了 bitmap。

```
统计uv： 一个网站的用户访问量，一个uid只记录一个，简单的话可以使用 zadd 记录uid，但数据量大的话会占用大量存储空间。

pfadd codehole user1
pfcount codehole
pfmerge 合并多个 key 的计数
```

### 布隆过滤器

bf.add/bf.exists 用于在大量的数据中判断是否存在某个元素，会存在一定误差，可以调整 bf.reverse(error_rare, initial_size)
错误率越低，所需要的空间越大。

![](/img/redis-bloom-filter.png)

每个布隆过滤器对应的数据结构就是 __一个大型的位数组__ 和 __几个无偏的 hash 函数__，无偏指的是能够把 hash 算的比较均匀。

向布隆过滤器添加 key 时，会使用多个 hash() 对 key 进行 hash得出一个整数的索引值，然后对位数组取模运算的到下标位置， 每个 hash() 都会得到不同的位置，再把这几个位都置为1 完成 add 操作。

查询存在的时候，操作一样，如果存在一个位为0，说明一定不存在。如果都为1，不一定存在，因为可能被 hash 冲突的原因可能被其他 key 修改为1。如果位数组比较稀疏，正确的概率就很大。如果数组拥挤，那么就容易误判。

##### 应用：

实现是基于 string 数据结构及位运算，可以解决业务层缓存穿透的问题，而且内存占用非常小，操作简单高校。

### GeoHash

GeoHash 算法将二维的经纬度数据映射到一维的整数（二分切法-将二维数据不断切分，最后用二进制标识），所有的数据都在一条线上。 内部结构为 zset

```
geoadd company 116.48105 39.996794 juejin
geoadd company 116.514203 39.905409 ireader

geodist company juejin ireader      计算距离

georadiusbymember company ireader 20km count 3 asc      计算范围20公里的单位
``` 

### keys/scan

keys 缺点：

1. 没有 offset、limit 参数限制，一次性会返回所有满足条件的 key
2. keys 会遍历所有的key，因为 Redis 是单线程模型，如果当前 key 数量过多，会导致其他指令阻塞。

scan：
1. 复杂度虽然为 O(n)，但通过游标分步进行的，不会阻塞线程
2. limit 限制返回数量
3. 返回的结果可能会有重复，需要客户端做去重
4. 返回的结果为空不一定说明没有该 key，具体看游标值是否为0

```
scan 0 math *@outlook.com count 1000
```

Redis 中所有的 key 都存在一个字典中，类似于 HashMap 数组+链表的结构，数组大小为2^n，扩容翻倍。
scan 返回的游标为数组的位置索引，称为槽（slot），limit 参数表示需要遍历的槽数，

scan 的遍历顺序采用了高位进位加法，考虑到了字典的扩容和缩容时避免槽位的遍历重复和遗漏。采用高位加法的遍历顺序，
rehash 后的槽位在遍历顺序上是相邻的。

![](/img/redis-rehash-slot.png)

```
从高位开始计算

1000
0100
0010
```

同时避免了扩容后对已遍历的槽位进行重复遍历。

为了避免像 Java 中 HashMap 扩容时，在数据量大时，线程出现卡顿的情况。Redis 采用了渐进式 rehash，同时保留
旧数组和新数组，然后定时任务中以及后续对 hash 的指令操作中渐渐地将旧数组中的元素迁移到新数组。

note: 在生产环境中，尽量避免大 key，因为大 key 在迁移的过程中会导致数据迁移卡顿，或者在扩容时，需要一次性划分
申请更大的内存，导致卡顿。
```
redis-cli -h 127.0.0.1 -p 7001 --bigkeys -i 0.1 每隔 100 条 scan 指令就会休眠 0.1s
```

### transaction

multi/exec/discard: 所有指令在 exec 之前不执行，而是缓存在服务器的事务队列中，直到exec指令，才开始执行事务队列。
当事务中间执行失败后，后续指令仍能继续执行。

watch 会在事务开始之前关注1或多个变量，当事务执行后，即服务器收到 exec 指令执行缓存的事务队列的时候，如果被 watch 
的变量被修改了，exec 会返回事务失败。

### SDS

Redis 字符串本质上就是一个字符数组，C 语言中的字符串是以 NULL 结尾的，`strlen()` 统计字符串长度的时候
采用遍历的方式计算，O(n)。 

```
struct SDS<T> {
    T capacity;     // 总容量
    T len;          // 长度
    byte flag;      // 特殊标记位
    byte[] content; // 数组
}
```

类似于 `ArrayList`，通过预分配冗余空间的方式，防止在字符串频繁变动的情况下，造成系统频繁地重新分配内存空间，
复制字符串数组的动作，减少了内存分配和复制带来的系统开销。

![](/img/redis-sds-struct.png)


```
redisObject {
    int4 type;
    int4 encoding;
    int24 lru;
    int32 refCount;
    void *ptr;
}
```

* embstr vs raw

embstr: 调用1次 `malloc`，redisObject 与 sds 内存上是连续的，embstr 整体上 64 byte，刚好和cpu cache line 一样，可以更好使用
内存。    
raw: 调用2次 `malloc`, redisObject 与 sds 内存上不连续


[redis-interview-collect](https://mp.weixin.qq.com/s/-y1zvqWEJ3Tt4h39Z0WBJg)