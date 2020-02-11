
### Redis Design

#### 单线程模型

1. 单线程能带来更好的可维护性，方便开发和调试（避免了锁的使用带来的复杂度）
2. 配合着 IO 多路复用，单线程有着很高的性能
3. 性能瓶颈不是 CPU，而是网络 IO


#### I/O 多路复用 

在 I/O 多路复用模型中，通过调用 `evport、epoll、kqueue、select` 等函数，同时监听多个文件描述符的可读写状态，当某些文件
描述符可读或者可写时，方法会返回可读以及可写的文件描述符个数

使用 I/O 多路复用模型能极大地减少系统地开销，系统不需要额外创建和维护进程和线程监听来自客户的大量连接，减少了服务器的
开发和维护成本。

#### 性能瓶颈

Redis 并不是 CPU 密集型的服务，如果不开启 AOF 备份，所有的 Redis 的操作都会在内存中完成不会涉及任何的 I/O 操作，处理速度
非常快。所以，整个服务的瓶颈在于网络传输带来的延迟和等待客户端的数据传输，也就是网络 I/O，所以多线程模型处理全部请求并不是
好的选择（同时会带来多线程切换时，上下文保存切换的问题）

Redis 的单线程模型对多核 CPU 没有完全利用，如果有这样的担心，在网络io 没有成为瓶颈时，那么可以采用 redis 分片机制，
在一台机器上部署多个 Redis 实例，充分利用 cpu 和 网卡的能力。

#### 阻塞

对于耗时的操作，基本上都是采用 fork 子进程的方式（AOF、RDB）。在最新的 Redis 几个版本中，加入了其它线程的异步处理的删除
操作，如 `UNLINK、FLUSHALL ASYNC、FLUSHDB ASYNC`等，当在 Redis 中存在超大的 kv 时，几十 MB 或者更大的数据并不能在几毫秒
的时间内处理完，Redis 可能会需要释放内存空间上的消耗比较多的时间，阻塞主进程中的其它操作。

导致阻塞的场景主要有：

* 不合理使用 API 和数据结构

例如在包含上万个元素的 hash 结构执行 `hgetall` 操作，由于数据量大且算法复杂度为 O(N)，导致执行效率慢。或者是在线上执行
 `keys` 这种全部查询的命令。可以通过慢查询的方式统计避免不合理使用 API：`slowlog get(n)`
 
* CPU 饱和的问题

单线程的 Redis 处理命令时，只能处理一个 CPU，如果 Redis 把单核的 CPU 使用率跑到接近 100%，则会出现导致 CPU 饱和的问题。
这时 Redis 将无法处理更多命令，严重影响吞吐量和系统的稳定性。

对于 CPU 饱和的问题，首先需要确定 Redis 的并发处理能否到达极限，排查时判断是否使用了高算法复杂度的命令，或者是否对内存
过度优化，如果 qps 确实很高，则需要考虑集群化水平扩展来分摊 qps 压力。

* 持久化阻塞

对于开启了持久化功能的 Redis 节点，需要排查是否是持久化导致的阻塞。比如有：fork阻塞、aof刷盘阻塞、hugePage写操作阻塞

1. fork 阻塞：fork 操作发生在 RDB 和 AOF 重写时，Redis 主线程调用 fork 操作产生共享内存的子进程，由子进程完成持久化
文件重写工作，如果 fork 操作本身耗时很长，必然会导致主线程阻塞。

2. aof刷盘阻塞：在开启 aof 持久化功能时，文件刷盘一般采用一秒一次，后台线程每秒对 aof 文件做 fsync 操作，当硬盘压力
过大时，fsync 操作需要等待完成，直到写入完成。如果主线程发现距离上一次的 fsync 成功超过2秒，为了数据安全性它会阻塞
直到后台线程执行 fsync 操作完成

3. 子进程在执行期间利用 linux 写时复制技术降低内存开销，因此只有写操作时 Redis 才复制需要修改的内存页，对于开启 
Transparent HugePages 的操作系统，每次写命令引起的复制内存页单位由 4K 变成 2M，放大了 512 倍，会拖慢了写操作的执行时间，
导致大量写操作慢查询。

* 外部原因

1. CPU竞争：与其它 cpu 密集型服务部署在一起，当其它进程过度消耗 cpu时，将严重影响 Redis 吞吐量
2. 内存交换：Redis 保证高性能的重要前提是所有的数据都在内存中，当 Redis 部分内存换出到硬盘时，由于内存和硬盘的读写速度
相差几个数量级，将导致 Redis 性能急剧下降。通常预防内存交换的方法是要将 Redis 实例设置最大内存，保证机器有足够的可用内存
3. 网络问题：链接拒绝、网络延迟、网卡软中断。


#### 清理策略

* Redis 配置项hz 定义了 ServerCron 任务的执行周期，默认为10，即 cpu 空闲每秒执行 10次
* 每次过期 key 清理的时间不超过 cpu 时间的 25%，即一次清理时间最大为 250ms，如果超过则退出清理，等待下次。
* 遍历db[]， 从db 中随机取 20 个key

#### 淘汰策略

allkeys-lru、volatile-ttl

* volatile-lru: 从已设置过期时间的数据集（server.db[i].expires）中挑选最近最少使用的数据淘汰
* volatile-ttl: 从已设置过期时间的数据集（server.db[i].expires）中挑选将要过期的数据淘汰
* volatile-random: 从已设置过期时间的数据集（server.db[i].expires）中选择任意数据淘汰
* allkeys-lru: 从数据集（server.db[i].dict）中挑选最近最少使用的数据淘汰
* allkeys-random: 从数据集（server.db[i].dict）中任意选择数据淘汰
* no-enviction: 禁止驱逐数据


### 数据丢失问题

* 异步复制导致的数据丢失

因为 master -> slave 的复制是异步的，所以可能有部分数据还没复制到 slave，master 就宕机了，此时这部分数据丢失。

* 脑裂丢失数据

脑裂，某个 master 所在的机器突然脱离了正常的网络，跟其他 slave 机器不能连接，但实际上 master 还运行着。此时哨兵
可能认为 master 宕机了，然后开始选举，将其它 slave 切换成了 master，这个时候，集群中就会存在两个 master。

此时虽然某个 slave 被切换成了 master，但 client 还没来得及切换成新的 master，还继续向旧 master 写数据。因此
旧 master 再次恢复的时候，会被作为一个 slave 挂到新的 master 上去，自己的数据会被清空，重新从新的 master 复制
数据，而新的 master 没有 client 后写入的数据，导致部分的数据丢失。


解决方法：

```
min-slaves-to-write 1
min-slaves-max-log 10

至少有一个 slave，数据复制和同步的延迟不能超过 10s
```

* 减少异步复制数据的丢失

通过 `min-slaves-max-log` 配置，一旦 slaves 复制数据和 ack 延时过长，就认为可能 master 宕机后损失的数据太多了，
那就拒绝写请求，这样就可以把 master 宕机时由于部分数据未同步到 slave 导致的数据丢失降低到可控范围之内。

* 减少脑裂的数据丢失

如果一个 master 出现脑裂，跟其他 slave 丢失连接，那么上面两个配置能确保说，如果不能继续给指定数量的 slave 发送数据，
而且 slave 超过 10s 没有给自己 ack 消息，那么就直接拒绝客户端的写请求，因此在脑裂场景下，最多就丢失 10s 数据。

### sdown & odown

* sdown 是主观下线，一个 sentinel 觉得一个 master 宕机（通过 ping master， 判断时间是否超过 `is-master-down-after-milliseconds`）
* odown 是客观下线，如果 quorum 数量 sentinel 都认为一个 master 主观下线（sdown）

### quorum & majority

* quorum

只有大于等于 quorum 数量的 sentinel 认为 master 主观下线，sentinel 集群才会认为 master 客观下线

* majority

majority 代表 sentinel 集群中大部分 sentinel 节点的个数，只有大于 `max(quorum, majority)` 个节点给某个 sentinel
 节点投票，才能确定该 sentinel 节点为 leader，majority 的计算方式为 `num(sentinels) / 2 + 1`，所以 sentinel 集群的
节点个数至少为3个，例如当节点个数为2时，如果一个sentinel 宕机，因为 majority 为 2，此时没有足够的 sentinel 选出 leader，
那么无法进行故障转移。

### master 选举条件

如果一个 master 被认为 odown 了，而且 majority 数量的哨兵都允许主备切换，那么某个哨兵就会执行主备切换操作，根据以下条件选择：

* 跟 master 断开连接的时长（如果 slave 与 master 断开连接时间超过 10 * `down-after-milliseconds`,将不适合）
* slave 优先级
* 复制 offset (如果 priority 相同，那么看 replica offset，offset越大说明已经复制了更多的数据，优先级越高)
* 如果上面两条条件相同，那么选择一个 run id 比较小的 slave


### redis cluster

在 redis cluster 模式下，每个 redis 都要开放两个端口，一个6397，另一个是 +1w，如16397，用于节点间的通信，
也就是 cluster bus，用来进行故障检测、配置更新、故障转移授权。cluster bus 使用了另一种二进制协议，`gossip`协议，
用于节点间进行高效的数据交换，占用更少的网络带宽和处理时间。

#### 节点间的内部通信

节点间的内部通信主要有以下两种：

* 集中式

集中式是将集群元数据（节点信息、故障等）集中存储在某个节点上。如多个 master 节点将数据交由 Zookeeper集群 对所有的元数据进行存储维护。

集中式的 __好处__ 在于，元数据的读取和更新时效性非常好，一旦数据出现了变更，就立即更新到集中式的存储中，其他节点读取的时候就可以感知到， 
__不好处__ 在于，所有的元数据的更新压力全部集中于一个地方，可能导致元数据的存储有压力。

* gossip协议

redis 维护集群元数据采用 `gossip`协议，所有节点都持有一份元数据，不同节点如果出现元数据变更，就不断将元数据发送给其他的节点，
让其他节点也进行元数据的变更。

gossip __好处__ 在于，元数据的更新比较分散，不是集中在一个地方，更新请求会陆续打在所有的节点上更新，降低了压力。 __不好在于__，
元数据的更新有延时，可能导致集群中的一些操作会有滞后。

#### gossip 协议

gossip 协议包含多种消息，包含 `ping`, `pong`, `meet`, `fail` 等等。

* meet: 某个节点发送 `meet` 给新加入的节点，让该新节点加入集群中，然后新节点就会开始与其他节点进行通信
* ping: 每个节点都会频繁地给其他节点发送 `ping`，其中包含自己的状态还有自己维护的集群元数据，互相通过 `ping` 交换数据
* pong: 返回 `meet` 和 `ping`，包含自己的状态和其他信息，也用于信息广播和更新
* fail: 某个节点判断另一个节点 fail 之后，就发送 `fail` 给其他节点，通知其它节点这个节点宕机的消息


[Redis Sentinel文档](https://redis.io/topics/sentinel)  