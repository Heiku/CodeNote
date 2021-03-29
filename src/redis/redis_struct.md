## Redis

### SDS

简单动态字符串(Simple Dynamic String)，除了用来保存数据库中地字符串之外，SDS 还被用作缓冲区（buffer）：AOF 模块中的 AOF 
缓冲区，以及客户端状态中的输入输出区，都是由 SDS 实现的。

```
struct sdsadr{
    int len;
    int free;
    char buf[];
}

| 'H' | 'E' | 'L' | 'L' | 'O' | '\0'|   |   |   |
free = 3
len = 5 
```

* free: 表示 SDS 的未使用空间
* len: 字符串的长度，O(1)，因为 C string 没有额外存储长度，所以C string len O(n)
* buf: char 数组，用于存储实际的字符，包括了 '\0' 结束空字符。

优点：
1. 杜绝了缓冲区溢出
   在 C string 有可能在s1 - s2 ，s1 在执行 strcat(dest, str) 的之前，忘记为 s1 分配足够的空间， str 有可能会溢出到 s2 的空间，
   导致 s2 保存的内容被意外地修改。sdscat() 会先判断空间是否足够，否则进行拼接操作。

2. 减少修改字符串时带来的内存重分配次数 为了避免 C string 在执行 strcat()/trim() 等操作会导致的 缓冲区溢出或是内存泄漏等问题，同时解决在扩容时的频繁内存分配字符串 空间的问题，SDS 采用了 free
   未使用空间（空间预分配策略）解决。  
   在扩展操作时，先检查未使用空间 free 是否足够，优先使用 free，避免了内存分配。在释放操作时，保留被释放的空间为 free， 以便下次扩展时使用，如果长时间未使用，可以使用 sds api 释放对应的 free
   空间避免了内存占用。

3. buf 可以保存二进制数据（二进制安全，不会对数据进行额外处理），同时兼容 C string

#### sds 节省空间的优化(最好看看链接图)

1. 当保存的是 long 类型整数时，RedisObject 中的指针将被直接替换成 int 数据，这样就不需要额外的指针指向整形数据，节省了指针带来的空间开销
2. 当保存的是字符串数据，且字符串长度 <= 44 字节时，RedisObject 中的（元数据 + 指针 + sds）是一块连续的内存区域，避免了内存碎片。embstr模式。
3. 当字符串大于 44 时，sds 的数据量开始变多，会给 sds 分配独立的空间，并用 RedisObject ptr 指向sds，raw模式。

[redis sds](https://time.geekbang.org/column/article/279649)

### List

`LRANGE integers 0 10` integers 列表键的底层实现就是一个链表，链表中的每个节点都保存了一个整数值。除了链表键之外， 发布与订阅、慢查询、监视器等功能也使用到了链表。

```
typedef struct listNode{
    struct listNode *prev;
    struct listNode *next;
    
    void *value;
}
```

```
typedef struct list{
    listNode *head;
    listNode *tail;
    unsigned long len;      // 链表中的节点数量
    void *(*dup)(void *ptr);    // 节点值复制函数
    void (*free)(void *ptr);    // 节点值释放函数
    int (*match)(void *ptr, void *key)      // 节点值比较函数
} list;
```

* 双端: 链表节点带有 prev 和 next 指针，获取某个节点的前置节点和后置节点的复杂度都是为 O(n)
* 无环: 表头节点的 prev 和 表尾节点的 next 都指向 NULL，对链表的访问以 NULL 为终点
* 头尾指针: 通过 list 结构的 head 和 tail 指针，获取链表的头节点和尾节点的复杂度为 O(1)
* 链表长度计数器: 通过 list 的 len 记录链表节点的数目，获取链表节点数目的复杂度为 O(1)
* 多态: 链表节点使用 void* 指针来保存节点值，并且可以通过 dup()/free()/match() 为节点值设置类型特定函数，所以链表
可以用于保存各种不同类型的值

### Dict

Redis 的字典使用哈希表作为底层实现，一个哈希表里面可以有多个哈希表节点，而每个哈希表节点就保存了字典中的一个键值对。

```
typedef struct dict{
    dictType *type;         // 类型特定函数
    void *prividata;        // 私有数据（保存了需要传给类型特定函数 dictType 的可可选参数）
    dictht ht[2];           // 哈希表
    int threhashidx;        // rehash 索引，当 rehash 不在进行时，值为 -1
}

typedef struct dictht{
    dictEntry **table;      // 哈希表数组
    unsignint long size;    // 哈希表大小
    unsignint long sizemask;    //  哈希表大小掩码，用于计算索引值
    unsignint long used;        // 哈希表已有的节点数量
} dictht;


typedef struct dictEntry{
    void *key;          // key
    union {
        void *val;
        unint64 _tu64;
        int64 _ts64;
    } v;
    struct dictEntry *next;     // next entry
} dictEntry;
```

dict 中的 ht 属性是一个包含两个项的数组，数组中的每个项都是一个 dictht 哈希表，一般情况下，字典只使用 ht[0] 哈希表， ht[1] 哈希表只会在对 ht[0] 哈希表进行 rehash 时使用。

和 Java HashMap 中类似，Redis 的哈希表使用了链地址法 (separate chaining) 来解决键冲突，每个哈希表节点都有一个 next 指针， 多个哈希表节点可以用 next
指针构成一个单向链表，被分配到同一索引上的多个节点可以用单向链表连接，解决了键冲突的问题。

#### 具体格式

1. 整个 redis 可以当作一个哈希表 ht
2. 一个键值对在底层实现对应一个 dictEntry 结构体
3. 键值在底层实现上都对应一个 RedisObject 结构体
4. RedisObject 结构体上记录了 type，encoding，ptr，ptr指向实际存储的数据结构

#### rehash

// 负载因子 = 哈希表已保存的节点数量 / 哈希表大小  
load_factor = ht[0].used / ht[0].size

当一下条件红的任意一个被满足时，程序会自动对哈希表进行扩容操作(RDB 和 AOF 期间禁止)：

1) 服务器目前没有在执行 `BGSAVE` or `BGREWRITEAOF` 命令，并且哈希表的负载因子 loadFactor >= 1
2) 服务器目前正在执行 `BGSAVE` or `BGREWRITEAOF` 命，并且哈希表的负载因子 loadFactor >= 5，已经快要到达上限，立即扩容

渐进式 rehash：rehash动作从 ht[0] -> ht[1], 整个过程是分多次、渐进式地完成地，因为当 hashmap 中保存地 entry 数据量大时，
一次性完成 entry 地转移需要大量地计算，可能导致服务在一段时间内停止服务。

具体操作：
在渐进式 rehash 期间，字典的删除，查找，更新等操作会在两个哈希表上进行，如果在 ht[0] 上查找不到，就去 ht[1] 上查找，
期间新添加的键值对一律被保存在 ht[1] 里面，ht[0] 不再进行任何添加操作，随着 rehash 操作，最终 ht[0] 会称为空表。


每次处理一个请求，从 ht[0] 第一个索引位置 index 开始，顺着这个索引位置 index 将该位置下的所有 entries 拷贝到 ht[1] 中，
当处理下一个请求时，从 index+1 开始重复这个过程。这样就将一次性拷贝的开销，分摊到了多次处理请求中，避免了耗时保证了数据的访问速度。

### SkipList

跳跃表 (skipList) 是一种有序的数据结构，它通过在每个节点中维持多个指向其他节点的指针，从而达到快速访问节点的目的。
跳跃表支持平均 O(logN), 最坏 O(N) 复杂度的查找，还可以通过顺序性操作来批量处理节点。

在 Redis 中，只有两个地方使用了跳跃表，一个是实现有序的集合键，另一个是在集群节点中作为内部结构。

```
typedef struct zskiplistNode{
    // 层
    struct zskiplistLevel{
        // 前进指针
        struct zskiplistNode *forward;
        // 跨度
        unsignint int span;
    } level[];

    // 后退指针
    struct zskiplistNode *backward;

    // 分值，用于排序
    double score;

    // 成员兑现
    robj *obj;
} zskiplistNode;
```

```
typedef struct zskiplist{
    structz skiplistNode *head, *tail;

    // 表中的节点数量
    unsignint long length;

    // 表中层数最大节点的层数
    int level;
} zskiplist;
```

### intSet

整数集合(intset) 是集合键的底层实现之一，当一个集合只包含整数值元素，并且这个集合的元素数量不多时，Redis 就会使用整数
集合作为集合键的底层实现。

```
typedef struct intset{
    // 编码方式
    uint32_t encoding; 

    // 集合包含的元素数量
    uint32_t length;

    // 保存元素的数组
    int8_t contents[];
} inset;
```

contents[] 的存储类型不是固定的，通常会按照encoding 规定的类型，但如果要存储的元素中类型对应，则需要对整个数组进行升级，
对应的位置上重新分配内存空间，将新的元素    添加到新的数组空间中。

这样处理的好处有：
* 提升灵活性：一个数组中可能同时存在 int16_t, int32_t, int64_t 多种类型
* 节约内存：如果要让一个数组同时保存 in16_t, int32_t, int64_t 这三种类型的值，最简单的方式就是直接创建 int64_t 的数组，
但这样的缺点就是当我们添加 int16_t, int32_t 时，数组仍然需要使用 int64_t 类型的空间保存，从而出现内存浪费的情况。


### zipList

压缩列表 (zipList) 是 Redis 为了节约内存而开发，是由一系列特殊编码的连续内存组成的顺序型（sequential）数据结构。一个
压缩列表可以包含任意多个节点（entry），每个节点可以保存一个字节数组或者一个整数值。

zipList 包括了 zlbytes, zltail, zllen, entry... , zlend，通过 zlbtes, zltail 可快速定位压缩列表的长度。
zipListNode 包括了 previous_entry_length, encoding, content，通常可以通过 zltail， previous_entry_length 可定位每个节点。
压缩的意义在于可以根据存储的 encoding 分配对应的存储空间，使得每个压缩列表的节点可以保存一个字节数组或者是一个整数值，这样节点的存储空间就能被
充分利用，很好地压缩了存储空间。


为什么说 zipList 能节省内存？  
zipList 采用了一段连续的内存空间存储数据，相比于 hashtable（dict）减少了内存占用碎片和指针内存占用，当节点较少时，zipList
更容易被载入到 CPU 缓存中。缺点是限制于数组链表结构，查找的时间复杂度为 O(N)，数据量小时可观，但数据量大时，不如 dict。

### redisObject

Redis 使用对象来表示数据库中的键和值，每次当我们在 Redis 中创建一个键值对时，至少会创建两个对象，一个键对象，一个值对象。

```
typedef struct redisObject{
    unsigned type: 4;       // 类型
    unsigned encoding: 4;   // 编码
    void *ptr;              // 指向底层数据结构的指针
}
```

type: REDIS_STRING, REDIS_LIST, REDIS_SET, REDIS_HASH, REDIS_ZSET， `type key` 查看

encoding:   
* REDIS_STRING: REDIS_ENCODING_INT（整型字符串）、REDIS_ENCODING_EMBSTR (embstr 编码的sds)、REDIS_ENCODING_RAW（sds）  
* REDIS_LIST: REDIS_ENCODING_ZIPLIST（压缩列表）、REDIS_ENCODING_LINKEDLIST（双端列表）
* REDIS_HASH: REDIS_ENCODING_HT（字典）
* REDIS_SET: REDIS_ENCODING_INTSET（整数集合）、REDIS_ENCODING_HT（字典）
* REDIS_ZSET: REDIS_ENCODING_ZIPLIST（压缩列表）、REDIS_ENCODING_SKIPLIST（跳跃表）

Redis 对对象划分 encoding，这样就可以根据不同的使用场景来为一个对象设置不同的编码，从而优化对象在某一个场景下的效率：  
例如，在列表对象包含的元素比较少时，Redis 使用压缩列表作为列表对象的底层实现
* 因为压缩列表比双端列表更节约内存，并且在元素数量较少时，在内存中以连续块的方式保存的压缩列表比起双端链表更快被载入到
缓存中
* 随着列表对象包含的元素越来越多，使用列表k空间保存元素的优势也逐渐消失，对象就会将底层实现从压缩列表转向功能更强、也更
适合保存大量元素的双端链表上。


#### 字符串对象

字符串对象的编码类型有： int、embstr、raw

embstr 编码是专门用于保存短字符串的优化编码方式，通常用于保存字符串值长度小于等于32字节，和 raw 编码一样，都使用了 redisObject 和 
sdshdr 结构表示字符串对象，但 raw 编码会调用两次内存分配函数来创建这两种结构，而 embstr 只会调用一次内存分配函数来分配一块连续的空间。
同理，在释放内存空间时，也只需调用一次内存释放函数。

浮点数 double 的存储是通过 embstr，计算后以 embstr 方式存储。

```
|    redisObject        |       sdshdr     |
| type | encoding | ptr | free | len | buf |

set、get、append、incrby、decrby、strlen、setrange、getrange
```

#### 列表对象

列表对象的编码类型有： zipList、linkedList

当列表对象可以同时满足以下两个条件时，列表对象使用 zipList 编码， 否则使用双端列表 linkedList 编码： 
* 列表对象保存的所有字符串元素的长度都小于 64 字节
* 列表对象保存的元素数量小于 512 个

```
redisObject (ptr -> zipList)
redisObject (ptr -> linkedList)

lpush、rpush、lpop、rpop、rindex、llen、linsert、lrlen、lset
```

#### 哈希对象

哈希对象的编码类型有: zipList、hashtable

zipList 编码的哈希对象使用压缩列表作为底层实现，每当有新的键值对要加入到哈希对象时，会先将保存了 key 的 zipListNode 加入到
压缩列表的表尾，再将保存了 value 的 zipListNode 推入到压缩列表的表尾。
* 保存了同一键值对的两个节点总是紧挨在一起，保存了键的节点在前，保存值的节点在后
* 先添加到哈希对象中的键值对会被放在压缩列表的表头方向，而后来添加的哈希对象中的键值对会被放在压缩列表的表尾方向。


```
redisObject( ptr -> zipList)
redisObject( ptr -> dict ) 

| zlbytes | zltail | zllen | "name" | "Tom | "age" | 25 | ... | zlend |
```

hashtable 编码的哈希对象使用了字典作为底层实现，哈希对象中的每个键值对都使用了字典键值对来保存
* 字典中的每个 key 都是一个字符串对象，对象中保存了键值对的键
* 字典中的每个 value 都是一个字符串对象，对象中保存了键值对的值

当哈希对象可以同时满足以下两个条件时，哈希对象使用 zipList 编码，否则使用 dict：
* 哈希对象保存的所有键值对的键和值的字符串长度都小于 64 字节
* 哈希对象保存的键值对数量小于 512 个

```
hset、hget、hexist、hdel、hlen、hgetall
```

#### 集合对象

集合对象的编码类型有：inset、hashtable

```
redisObject (ptr -> inset)
redisObject (ptr -> dict)
```

当集合对象可以同时满足一下两个条件时，对象使用 inset 编码，否则使用 hashtable：
* 集合对象保存的所有元素都是整数值
* 集合对象保存的元素数量不超过 512 个

```
sadd、scard、sismember、smembers、srandmember、spop、srem
```

#### 有序集合对象

有序对象的编码类型有：zipList、skipList

zipList 编码的压缩列表对象使用压缩列表作为底层实现。每个集合元素使用两个紧挨在一起的压缩列表节点来保存，第一个节点
保存元素的成员（member），第二个元素保存元素的分值（score）。压缩列表内的元素按分值从小到大进行排序，分值较小的元素
在表头，分值大的元素在表尾。

```
typedef struct zset{
    sskiplist *zsl;
    dict *dict;
}

zlbytes|zltail|zllen| "banana" | 5.0 | "cherry" | 6.0 | "apple" | 8.5 | zlend |
```

zset 结构同时使用了跳跃表和字典来保存有序集合元素，但这两种数据结构都会通过指针来共享相同元素的成员和分值，所以同时使用
跳跃表和字典来保存集合元素不会产生任何重复成员或者分值，也不会因此而浪费额外的内存占用。

那么，为什么需要同时使用跳跃表和字典来实现呢？  
* 这是从性能方面考虑的，因为有序集合完全可以单独使用字典或者跳跃表实现，但这样单独使用的话存在这性能的降低。
例如，如果我们只使用字典实现zset，当查找的时候，时间复杂度仅为 O(1)，但因为字典的无序保存形式，比如在 zrank、zrange
等命令，需要进行排序 O(NlongN)。  
如果我们只用跳跃表实现 zset，那么执行范围操作的有点因为跳跃表的特性将降低，但查找值的时候复杂度将从 O(1)->O(logN)

当有序集合对象可以同时满足以下两个条件时，对象使用 zipList，否则使用 skipList：
* 有序集合保存的元素数量小于 128 个
* 有序集合保存的所有元素成员的长度都小于 64 字节

```
zadd、zcard(return list node num includes value & score)、zcount、zrange、zrevrange、zrank、zrerank、zrem、zsocre
```

[Redis skipList](http://zhangtielei.com/posts/blog-redis-skiplist.html)

#### 内存引用

C 语言并不具备自动内存回收功能，所以 Redis 在自己对象系统中构建了一个引用计数 (reference counting) 技术实现的内存回收机制。
程序可以跟踪对象的引用计数信息，在适当的时候自动释放对象并进行内存回收。

```
typedef struct redisObject{
    ...
    int refcount;       // 引用计数
}

incrRefCount、decrRefCount、resetRefCount
```

创建对象时，初始化为1，使用+1，不使用-1，当为0时，对象所占用的内存会被释放。

#### 对象共享

对象的引用技术属性 refCount 不仅用于内存回收机制，同时还有对象共享的作用，当相同键的对象引用相同的值对象，那么该值对象的
引用计数值 refCount + 1，这样可以大大的减少内存的创建占用等。但受限于字符串对象（特别是多值的）在检测对象是否相同时，会
消耗很多的 CPU，所以Redis 只会对包含整数值的字符串对象进行共享。

#### 对象空转时间

redisObject 除了 type、encoding、ptr、refCount 之外，还有一个属性为 lru，记录了对象最后一次被命令程序访问的时间。

```
typedef struct redisObject{
    ...
    unsigned lru;
}

obejct idletime key
```

如果服务器打开了 maxmemory 选项，并且服务器用于回收内存的算法为 volatile-lru 或者是 allkeys-lru，那么当服务器
占用的内存超过了 maxmemory 设置的上限值时，空转时长较高的那部分键会优先被服务器释放，从而回收内存。




