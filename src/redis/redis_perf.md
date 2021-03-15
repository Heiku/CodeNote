
# Redis 性能排查



## 确定是否是 Redis 导致变慢

1. 可以使用 Cat、SkyWalking 这类 APM 工具，链路追踪，分析是哪一环调用时间超时、慢等情况
2. 业务服务器与 Redis 服务器之间存在网络问题，比如网络线路质量不佳，网络数据包在传输时存在延迟、丢包等
3. Redis 本身问题，进一步排查，例如 __基准测试__



#### 基准测试

在生产服务器上测试 Redis 的最大相应延迟和平均响应延迟



1. 测试实例 60s 内的最大响应延迟

   ```
   redis-cli -h 127.0.0.1 -p 6379 --intrinsic-latency 60
   ```

2. 查看一段时间（间隔）内 Redis 的最大、最小、平均延迟

   ```
   redis-cli -h 127.0.0.1 -p 6379 --lantency-history -i 1
   ```

   

## 使用复杂度过高的命令



可以先打开慢日志 （slowlog），记录了哪些命令统计耗时超出预定值

```
# 命令执行超过5毫秒
CONFIG SET slowlog_slower_than 5000
# 记录的最大数目
CONFIG SET slowlog_max_len 500
# 查询最近10条慢日志
SLOWLOG GET 10
```



Redis 命令超时特点：

1. 使用 O(n) 以上的复杂度的命令，例如 SORT、SUNION、ZUNIONSTORE 等聚合类命令
2. 使用 O(n) 复杂度命令，但 N 的值非常大，会导致收集的数据量比较多，响应慢



由于 Redis 是单线程处理执行命令，如果正在处理的命令请求慢，会导致后面请求排队，整体访问性能下降，即如果有 CPU耗时的命令，建议不在 Redis 上进行计算，而是返回客户端后在客户端上完成数据计算（聚合、整理）。



## Big Key

Redis 在写入数据时，需要为新的数据分配内存，相对应的，当从 Redis 中删除数据时，释放对应的内存空间。



如果一个 key 写入的 value 非常大，那么在分配内存时会比较耗时，同样，在删除这个 key 时，释放内存对象也会增加耗时

```
# 查看 bigkey 分布情况
redis-cli -h 127.0.0.1 -p 6379 --bigkeys -i 0.01

（线上慎用）本质上是使用 scan 命令遍历查找 keys，根据各个 key 的类型进行统计
1. 线上使用 bigkey 扫描的时候，Redis OPS 突增，为了减小性能损耗，-i 执行扫描间隔
2. 扫描的结果对于(list、set、zset、hash) 只是代表元素数量多，并不是内存占用，需要具体情况具体分析
```



1. 业务层面应该避免使用写入 bigkey
2. Redis 4.0 可以使用 __UNLINK__ 代替 DEL，降释放内存的操作放到后台线程
3. Redis 6.0 可以开启 `lazy-free` 机制（lazyfree-lazy-user-del = true），执行 DEL 的操作会被放入到后台线程



## 集中过期

特点：在某个时间点突然出现一波延时，Redis 访问变慢时间点很有规律，例如（整点、每隔一段时间）出现一波延迟



#### 过期数据删除策略

1. 被动删除：当访问某个 key 的时候才会去检查，如果过期则删除
2. 主动删除：Redis 内部维护了一个定时任务，每隔100ms（1s 10次）从全局过期 ht 中随机取出20个 key，然后删除过期的key，每次删除完成后检测过期 key 比例是否超过了 25%，如果超过重复此过程，如果本次删除任务超过 25ms，才会退出循环。



解决办法：

1. 集中过期 key 增加一个随机过期时间，把集中过期的时间打散，降低 Redis 清理过期 key 的压力
2. Redis 4.0+ 启动 `lazy-free` ，删除时会将操作放入后台线程执行



## 实例内存上限



__allkeys-lru、volatile-lru__ ：每次从实例中随机取出一批 key，然后淘汰一个最少访问的 key，之后把剩下的 key 暂存到一个池子中，继续随机取一批 key，并与之前的 key 比较，继续淘汰，反复这个过程，直至实例的内存低于 maxmemory 以下。



优化：

1. 还是避免大 key，大 key 的释放在这个过程会很耗时
2. 拆分实例，将淘汰的 key 压力分摊在多个实例上
3. lazy-free



## fork

fork：主进程创建子进程的过程中，需要拷贝自己的内存页表给子进程，如果实例很大，会放发这个过程

```
# 查看上次 fork 时间点
info

lastest_fork_usec:
```



优化：

1. 控制实例内存：10G以下，实例越大，约有可能因为节省内存执行 aof rewrite 造成 fork，也有可能在新从节点连接的时候产生 RDB 造成 fork
2. 持久化策略：在 slave 节点执行 RDB 备份（流量少时执行），非敏感数据可以关闭 AOF 和 AOF REWRITE
3. 降低主从库全量同步的概率：适当调大 `repl-blacklog-size` 参数，避免主从全量同步



## AOF

appendfsnyc everysec：（主线程和子线程双写盘）如果后台线程由于负载过高，导致 fsync 发生阻塞，迟迟不能返回，那么主线程在执行 write 系统调用时，也会被阻塞住，直到后台线程 fsync 执行完成后，主线程执行 write 才能成功。（AOF 赶上了 AOF rewrite）



优化：

1. 开启 `no-appendfsync-on-rewrite = yes`，即在 aof rewrite 期间，将刷盘策略改成了 appendfsync none，虽然避免了磁盘的阻塞，但在 aof rewrite 期键会 __丢失数据__