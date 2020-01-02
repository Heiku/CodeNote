
### 发布与订阅

Redis 的发布与订阅由 PUBLISH、SUBSCRIBE、PSUBSCRIBE 等命令组成。通过 SUBSCRIBE 命令可订阅一个或多个频道，从而成为这些
频道的订阅者，当其它客户端向被订阅的频道发送消息（message）时，频道的所有订阅者都会收到这条消息。通过命令 PUBLISH 可发送
频道信息。

```
struct redisServer{
    ...
    dict *pubsub_channels;      // 保存所有频道的订阅关系

    list *pubsub_patterns;      // 保存所有模式订阅关系
}

news.it -> client1, client2, client3
```

#### 事务

Redis 通过 MULTI、EXEC、WATCH 等命令实现事务（transaction）功能。事务提供了一种将多个命令打包，然后一次性、按顺序地执行
多个命令机制，并且在事务执行期间，服务器不会中断事务而改去执行其它客户端命令请求。只会在事务中地所有命令执行完毕后，才会
去处理其它客户端地命令请求。

每个客户端都有自己地事务状态：
```
typedef struct redisClient{
    ... 
    multiState mstate;      // 事务状态

} redisClient;

typedef struct multiState{
    // 事务队列，FIFO
    multiCmd *commands;

    // 已队命令计数
    int count;
} multiState;

typedef struct multiCmd{
    // 参数
    robj *argv;

    // 参数数量
    int argc;

    // 命令指针
    struct redisCommand *cmd;
} multiCmd;
```

然后当一个处于事务状态的客户端向服务端发送 EXEC 命令时，这个 EXEC 命令将被立即执行。服务器会遍历这个客户端事务队列，
执行队列中保存的所有命令，最后将执行命令的结果全部返回客户端。

#### Watch

WATCH 命令是一个乐观锁（optimistic locking），它可以在 EXEC 命令执行之前，监视任意数量的数据库键，并在 EXEC m命令执行时，
检查被监视的键是否至少有一个被修改过了，如果是的话，服务器将拒绝执行事务，并向客户端返回代表事务执行失败的空回复。

```
typedef struct redisDb{
    ...
    dict *watched_keys;         // 正在被 watch 命令监视的键
} redisDb;
```

对数据库修改的命令，都会到 watched_keys 字典检查，查看是否有客户端正在监视刚刚被命令修改过的数据库键，如果有的话，
那么调用 `touchWatchKey()` 将监视被修改键的客户端的 `REDIS_DIRTY_CAS` 标识打开，表示该客户端的事务安全性已经被破坏。

#### 判断事务是否安全

当服务器接收到一个客户端发来的 EXEC 命令，服务端会判断这个客户端是否打开了 `REDIS_DIRTY_CAS` 标识，如果客户端已经打开，
那么说明客户端所监视的键中，至少有一个键已经被修改过了，在这种情况下，客户端提交的事务已经不再安全，所以服务器会拒绝
执行客户端提交的事务。


#### ACID

Redis 的事务与传统的关系型数据库事务的最大区别在于：Redis 不支持事务回滚机制（rollback），即使事务队列中的某个命令
在执行期间出现了错误，整个事务也会继续执行下去，直到事务队列中的所有命令都执行完毕为止。


### SORT

SORT 命令为每个被排序的键都创建一个与键长度相同的数组，数组的每个项都是一个 redisSortObject 结构，根据值得类型不同，
进行排序。

```
typedef struct _redisSortObject{
    // 被排序键的值
    robj *obj;
    
    // 权重
    union{

        // 排序数字值使用
        double score;

        // 排序带有 BY 选项的字符串值使用
        robj *cmpobj;
    } u;
} redisSortObject;
```


### 慢日志

Redis 的慢日志功能用于记录执行时间超过给定时长的命令请求，用户可以通过这个功能产生的日志监视和优化查询速度

```
slowlog-log-slower-than: 指定执行时间超过多少微秒
slowlog-max-len: 选项指定服务器最多保存多少条慢日志查询日志
```

### 监视器

通过执行 MONITOR 命令，客户端可以将自己变成一个监视器，实时地接收并打印服务器当前处理地命令请求相关信息。

```
def MONITOR():
    client.flags |= REDIS_MONITOR       // 打开客户端地监视器标志

    server.monitors.append(client)      // 将当前客户端加入到服务器地 monitors 列表中

    send_reply("OK")                    // 向客户端返回 OK
```