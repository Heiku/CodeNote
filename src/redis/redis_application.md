

### Redis Application

#### 基础数据结构

##### string

Redis 字符串采用的是动态字符串（sds），可以修改的字符串。内部结构实现上类似于 Java 中的 ArrayList，采用预分配冗余空间的方式
减少内存的频繁分配。（键值对、计数）

##### list

Redis 列表相当于 Java 中的 linkedList(quickList/zipList)，而不是数组，所以在插入效率为 O(1)，但在删除查找为O(N),

##### hash

类似于 Java 中的 HashMap，同样采用链表法存储碰撞的的元素。Redis 中的 rehash 采用的是渐进式的 rehash，rehash的同时，
保留新旧两个 hash 结构，查询同时查两个结构，知道迁移的线程完成任务才替代。

##### Set

类似于 Java 中的 HashSet，字典中的所有 value 都是一个 NULL。

##### ZSet

类似与 Java 中 SortedSet 和 HashMap 的结合。使用Set保证了 Value 的唯一性，使用map存储 value 对应的 score。
内部通过跳跃表实现。


#### HyperLogLog

pfadd/pfcount: 分别为增加计数、获取计数。 存在一定的误差 0.8%。底层使用了 bitmap。

```
统计uv： 一个网站的用户访问量，一个uid只记录一个，简单的话可以使用 zadd 记录uid，但数据量大的话会占用大量存储空间。

pfadd codehole user1
pfcount codehole
pfmerge 合并多个 key 的计数
```

#### 布隆过滤器

bf.add/bf.exists 用于在大量的数据中判断是否存在某个元素，会存在一定误差，可以调整 bf.reverse(error_rare, initial_size)
错误率越低，所需要的空间越大。

![](/img/redis-bloom-filter.png)

每个布隆过滤器对应的数据结构就是 __一个大型的位数组__ 和 __几个无偏的 hash 函数__，无偏指的是能够把 hash 算的比较均匀。

向布隆过滤器添加 key 时，会使用多个 hash() 对 key 进行 hash得出一个整数的索引值，然后对位数组取模运算的到下标位置，
每个 hash() 都会得到不同的位置，再把这几个位都置为1 完成 add 操作。

查询存在的时候，操作一样，如果存在一个位为0，说明一定不存在。如果都为1，不一定存在，因为可能被 hash 冲突的原因可能被其他 key 
修改为1。如果位数组比较稀疏，正确的概率就很大。如果数组拥挤，那么就容易误判。




[redis-interview-collect](https://mp.weixin.qq.com/s/-y1zvqWEJ3Tt4h39Z0WBJg)