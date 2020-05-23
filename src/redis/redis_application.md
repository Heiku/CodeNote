

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



[redis-interview-collect](https://mp.weixin.qq.com/s/-y1zvqWEJ3Tt4h39Z0WBJg)