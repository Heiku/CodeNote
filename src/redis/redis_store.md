### 数据库

数据库的键空间是一个字典，所以针对数据库的操作，如添加，删除都是通过对键空间字典进行操作实现。

```
struct redisServer{
    redisDb *db;        // 一个数组，保存着服务器中的所有数据库 db[0],db[1],db[2]...
    int dbnum;          // 数据库数量
}

struct redisClient{
    redisDb * db;       // 记录客户端正在使用的数据库
}

typedef struct redisDb{
    ... 
    dict *dict;         // 本质上，使用字典结构作为数据库的键空间
}
client 可以通过 select index，切换数据库
```

#### 设置过期时间

设置过期时间的方式有：expire(s), pexpire(ms), expireat(timestamp s), pexpireat(timestamp ms)，但最后都会统一转换成
 pexpireat(timestamp ms) 的方式存储时间。
 
Redis 中的 redisDb 结构使用了 expires dict 保存数据库中所有键的过期时间，被称为过期字典。
* 过期字典中的 key 是一个指针，指向键空间中的某个键对象（具体的数据key）
* 过期字典中的 value 是一个 long long 类型的整数，这个整数保存了键所指向的数据库键的过期时间（ms unix timestamp）

```
typedef struct redisDb{
    ...
    dict *expires;      // 过期字典，保存着键的过期时间
}
```

#### 过期键的删除策略

* 定时删除：在设置键的过期时间的同时，创建一个定时器（timer），让定时器在键的过期时间执行对键的删除操作
* 惰性删除：放任键过期不管，但当访问该键的时候，都检查取得的键是否过期，如果过期，删除该键。如果没过期，返回键信息
* 定期删除：每隔一段时间，程序就对数据库进行一次检查，删除里面的过期键。至于删除多少过期键，检查多少数据库，依照算法决定

定时删除和定期删除属于主动删除，而 惰性删除属于被动删除策略。

##### 定时删除

定时删除策略对内存是最友好的，通过使用定时器，定时删除策略能保证过期的键能尽快被删除，并释放过期键所占用的内存。另一方面，
定时删除策略的缺点是 _会占用 CPU 时间_ ,在内存不紧张但是 CPU 时间非常紧张的情况下，将 CPU 时间用在删除和当前任务无关
的过期键上，无疑会对响应时间和吞吐量造成影响。

除此之外，创建定时器timer 需要用到 Redis 中的时间事件，而时间事件的实现方式 - 无序链表，查找一个时间事件的事件复杂度为
0(N)，并不能高效地处理大量时间事件。因此定时删除策略并不现实。


##### 惰性删除

惰性删除策略对 CPU 时间来说是最友好的，程序只会在取出键才会对键进行过期检查，并且删除的目标仅限于当前处理的键，这个策略
不会在删除其他无关的过期键上花费任何的 CPU 时间。但它对内存最不友好，如果任何一个键过期，只要它不被访问，那么这个键将不会
被删除，即它所占用的内存空间将不会被释放。

例如日志数据，记录后并不会被访问到，在数据库中存在非常多的过期键，那么它们将不会被删除（除非 FLUSHDB），无用的数据占用
了非常多的内存空间，产生了内存泄漏。

##### 定期删除

定期删除策略是前两种策略的一种整合分析：
* 定期删除策略每隔一段时间执行一次删除过期操作，并通过限制删除操作执行的时长和频率来减少删除操作对 CPU 时间的影响
* 同时，通过定期删除过期键，定期删除策略有效地减少了因为过期键而带来地内存浪费

但定期删除策略地难点在于确定删除操作执行的时长和频率：
* 如果删除操作执行地太频繁，或者执行地时间太长，定期删除策略就会退化成定时删除策略，以至于将 CPU 时间过多地消耗在删除
过期键上面
* 如果删除操作执行的太少，或者执行的时间太短，定期删除策略又会和惰性删除策略一样，出现浪费内存的情况

##### 过期删除实现

在 Redis 中，实际使用的是 _惰性删除_ 和 _定期删除_ 这两中策略，通过两种策略的配合，服务器可以很好地合理使用 CPU 时间
和避免内存空间之间取得平衡。

* 惰性删除策略

惰性删除策略由 `db.c/expireIfNeeded` 函数实现，读写数据库的命令在执行之前都会调用 `expireIfNeeded()` 对键进行检查

* 定期删除策略

定期删除策略由 `redis.c/activeExpireCycle` 函数实现，每当 Redis 服务器周期性操作 `redis.c/serverCron` 函数时，
`activeExpireCycle()` 都会被调用，在规定的时间内，多次遍历服务器中的各个数据库，从数据库中的 expires 字典中随机检查
一部分键的过期时间，并删除其中的过期键。


##### AOF、RDB、复制对键过期处理

1. RDB
在执行 SAVE 或者是 BGSAVE 创建一个新的 RDB 文件时，程序会对数据库中的键进行检查，已过期的键不会保存在新创建的 RDB 文件中。
而在载入 RDB 文件时，如果为主服务器，那么在载入过程中会忽略过期键，保证载入的键都是可用的。而对于从服务器而言，在载入
 RDB 文件时，文件中保存的所有键，无论是否过期，都会被载入到数据库中。不过，因为主从服务器在进行数据同步时，从服务器的数据
会被清空，所以一般过期键的载入对从服务器影响不大。

2. AOF
当服务器以 AOF 持久化模式运行时，如果数据库中的某个键已经过期，但还没被惰性删除或定期删除，那么 AOF 不会做任何操作。
但当过期键被删除后，程序会向 AOF 文件追加（append）一条 DEL 命令，显式记录该键已经被删除。

3. 复制
当服务器运行在复制模式下，从服务器的过期键删除动作都由主服务器控制，保证了数据地一致性：
* 主服务器在删除一个过期键之后，会显式地向所有从服务器发送一个 DEL 命令，告知从服务器删除这个过期键
* 从服务器在执行客户端发送过来地读命令时，即使碰到过期键也不会将过期键删除，而是继续像处理未过期地键一样处理过期键
* 从服务器只有在主服务器发来地 DEL 命令之后，才会删除过期键
fixed: upgrade 3.2 or TTL



### RDB (Redis Database Backup file)

RDB 持久化功能所生成的 RDB 文件是一个经过压缩的二进制文件，通过该文件可以还原生成 RDB 文件时的数据库状态。
* SAVE: SAVE 命令会阻塞 Redis 服务进程，直到 RDB 文件创建完毕，阻塞期间，服务器不能处理任何命令请求
* BGSAVE: BGSAVE 命令会 fork() 生成子进程，然后由子进程负责创建 RDB 文件，服务进程 (父进程) 继续处理命令请求

RDB 文件的载入工作是在服务器启动时自动执行的，只要 Redis 服务器在启动时检测 RDB 文件存在，就会自动载入 RDB 文件。
而且，因为 AOF 文件的更新频率比 RDB 文件的更新频率高，所以如果服务器开启了 AOF 持久化功能，那么服务器会优先使用 AOF 文件
还原数据库状态，只有当 AOF 持久化功能关闭时，服务器才会使用 RDB 文件还原数据库状态。

```
BGSAVE 的命令使用：
save 900 1      // 900s 内至少一次修改
save 300 10     // 300s 内至少10次修改
save 60 10000   // 60s 内至少10000次修改
```

#### 保存条件

Redis 服务器周期性操作函数 serverCron，默认每隔 100ms，该函数用于对正在运行的服务器进行维护，其中一项工作就是检查 save
 选项所设置的保存条件 saveparam[] 中是否有条件满足，如果满足，执行 BGSAVE 命令

```
struct redisServer{
    ...
    struct serverparam *saveparam;      // 条件数组 saveparam[0], saveparam[1] ...

    long long dirty;            // 修改计数器
    time_t lastsave;            // 上一次执行保存时间
}

struct saveparam{
    time_t seconds;         // 秒数
    int changes;            // 修改数
}
```

#### RDB 文件结构

* REDIS: 用于在载入文件时，快速检查所载入的文件是否时 RDB 文件
* db_version: 记录了 RDB 文件的版本号，长度为 4 个字节
* databases: 包含着零个或任意多个数据库，以及各个数据库中的键值对数据
* EOF: 1字节的常量，标志着 RDB 文件正文内容的结束，当读取到这个值时，说明所有的键值对已经被载入完毕
* check_sum: 校验和，长度为 8字节，通过 REDIS、db_version、databases、EOF 这四个部分计算得出，在载入时，会重新计算校验和
并与记录的 check_sum 进行比对，以此来检查 RDB 文件是否出错或者损坏


```
REDIS | db_version | databases | EOF | check_sum |

databases: | SELECTDB | db_number | key_value_pairs | 

key_value_pairs: | EXPIRETIME_MS | ms | TYPE | key | value |  
```


### AOF (Append Only File)

与 RDB 持久化保存数据库中的键值对来记录数据库状态不同，AOF 持久化是通过保存 Redis 服务器所执行的 _写命令_ 来记录数据库状态。
在服务器启动时，可以通过载入和执行 AOF 文件中保存的命令来还原服务器关闭之前的数据库状态。

AOF 持久化功能的实现可以分为命令追加（append）、文件写入、文件同步（sync）三个步骤

#### 命令追加

```
struct redisServer{
    ...
    // AOF 缓冲区
    sds aof_buf;    
}
```

#### 写入 & 同步

Redis 服务器进程就是一个事件循环（loop），这个循环中的文件事件负责接收客户端的命令请求，以及向客户端发送命令回复，
而时间事件则负责执行像 `serverCron()` 这样的定时函数

服务器在处理文件事件时，可能会执行写命令，使得一些内容被追加到 aof_buf 缓冲区里面，所以在服务器每次结束一个事件循环之前，
它都会调用 `flushAppendOnlyFile()` ，考虑是否需要将 aof_buf 缓冲区中的写入和保存到 AOF 文件里面

```
def evenLoop(){
    while True:
        // 接收文件事件，包括了接收命令及发送回复命令
        // 处理命令请求时会将命令内容追加写到 aof_buf 缓冲区中
        processFileEvents();
        
        // 处理时间时间, 例如 serverCron() 定期扫描过期键等
        processTimeEvents();
        
        // 考虑是否将 aof_buf 中的命令数据保存文件中，取决于 appendfsync 属性
        flushAppendOnlyFile();
}
```

`flushAppendOnlyFile()` 的具体行为由 `appendfsync` 属性决定：
* always: 将 aof_buf 缓冲区中的所有内容写入并同步到 AOF 文件（最安全，效率最低）
* everysec: 如果上次同步 AOF 文件的时间距离现在超过 1s，那么对 AOF 文件进行同步操作，由同步操作额外的线程进行 (效率高，
且逻辑上只丢失一秒的数据)
* no: 只写入，并不对 AOF 文件进行同步，何时同步由操作系统决定 （存在丢失命令数据的风险）


##### 文件写入与同步

当用户调用 `write()`，将数据写入到文件时，操作系统通常会将写入数据暂时保存在一个 _内存缓冲区_ 里面，等待缓冲区的空间
被填满、或者超过指定的时限后，才真正地将缓冲区中地数据写入到磁盘中。这样地操作虽然提升了效率，但当计算机停机时，
保存在内存缓冲区里面地写入数据也会丢失。

为此，系统提供了 `fsync()` 和 `fdatasync()` 两个同步函数，它们可以强制让操作系统立即将 _内存缓冲区_ 中的数据写入到磁盘中，
从而确保了写入数据的安全型。


#### AOF 载入 & 数据还原

1. 创建一个伪客户端（fake client），因为 Redis 命令只能在客户端上下文中执行，而载入 AOF 文件时命令来自文件而不是网络
2. 从 AOF 文件中分析并读取一条写命令
3. 使用伪客户端执行被读出的写命令
4. 循环

#### AOF 重写

随着服务器运行时间的流逝，AOF 文件中的内容会越来越多，文件的体积也会越来越大，如果不加以控制，体积过大的 AOF 文件可能
会对 Redis 服务器、甚至宿主计算机造成影响，而且体积越大的 AOF 文件，还原所需的时间就越多。

AOF 重写的实现并不依据 “旧 AOF 文件”，而是读取服务器中的数据库数据状态，采用命令的方式记录到新的 AOF 文件中。例如：  
```
RPUSH dev "C"
RPUSH dev "C++"
... 

这样多条list命令，重写的时候会直接读取这个 key 下的所有数据，然后重写命令为
RPUSH dev "C" "C++" ....

这样就实现了 AOF 文件的重写，避免了多行的 RPUSH 命令记录
```

##### AOF 后台重写

AOF 重写程序 `aof_rewrite()` 可以很好地完成创建一个新的 AOF 文件的任务，但这个函数会进行大量的写入操作，而调用这个函数
的线程将会被长时间阻塞，所以 Redis 采用了 `fork()` 子进程的方式进行 AOF 重写。

* 子进程进行 AOF 重写期间，服务器进程（父进程）可以继续处理命令请求
* 子进程带有服务器进程的数据副本，使用了子进程而不是线程，可以避免在使用锁的情况下，保证数据的安全性


为了保证重写过程中写入的命令与当前数据库状态一致，AOF 分为 AOF 缓冲区和 AOF 重写缓冲区，当在重写过程中时，写命令会同时
追加写入到 aof_buf， rewrite_aof_buf 中，在子进程完成 AOF 重写工作后，它会向父进程发送一个信号，父进程在接收到该信号之后，
会调用一个信号处理函数，包括以下工作：
1) 将 AOF 重写缓冲区中的所有内容写入到新的 AOF 文件中，这时新 AOF 文件所保存的数据库状态和服务器当前的数据库状态一致。
（因为这时候回到了主进程，所以这时会阻塞写入 AOF 缓冲区的操作，保证了会将 AOF 缓冲区中的写入命令完全写入文件）
2) 对新的 AOF 文件进行改名，原子地（atomic）覆盖现有的 AOF 文件，完成新旧两个 AOF 文件的替换。

整个 AOF 重写过程中，只有信号处理函数执行的时候会对服务器进程（父进程）造成阻塞，其他时候都不会阻塞父进程，这样将 AOF 
重写对服务器性能照成的影响降到最低。


### 事件

Redis 服务器是一个事件驱动程序，服务器需要处理以下两类事件：
* 文件事件（file event）：Redis 服务器通过套接字与客户端进行连接，而文件事件就是服务器对套接字操作的抽象。服务器与客户端
的通信会产生相应的文件事件，而服务器则通过监听并处理这些事件来完成一系列网络操作。
* 时间事件（time event）：Redis 服务器中的一些操作（如 serverCron()）需要在特定时间点执行，而时间事件就是服务器对这类定时
操作的抽象。

#### 文件事件

Redis 基于 Reactor 模式开发了自己的网络事件处理器，被称为文件事件处理器（file event handler）：
* 文件事件处理器使用了 _I/O 多路复用_ （multiplexing）程序来同时监听多个 socket，并根据 socket 目前执行的任务来为套接字
关联不同的事件处理器
* 当被监听的 socket 准备好执行连接应答（accept）、读取（read）、写入（write）、关闭（close）等操作时，与操作对应的文件事件
就会产生，这时文件事件处理器就会调用 socket 之前关联好的事件处理器来处理这些事件。

虽然文件事件处理器以单线程方式运行，但通过 I/O 多路复用程序监听多个 socket，文件事件处理器既实现了高性能的网络通信模式，
又可以很好地与 Redis 服务器中其他以单线程方式运行地模块进行对接，保持了 Redis 内部单线程设计地简单性。

##### 文件事件处理器

文件事件处理器由四部分组成：socket、I/O 多路复用程序、文件事件分派器（dispatcher）、事件处理器。

* 文件事件是对 socket 的抽象，每当一个 socket 准备好执行应答（accept）、写入、读取、关闭等操作时，就会产生一个文件事件。  
* I/O 多路复用程序负责监听多个 socket，将所有产生事件的 socket 放入到一个队列中，通过这个队列，以有序地（sequentially），
同步（synchronously）每次一个 socket 的方式向文件事件分派器传送 socket。
* 文件事件分派器接收 I/O 多路复用传来的 socket，并根据 socket 产生的事件类型，调用对对应的事件处理器。

```
socket(s1, s2... ) -> I/O 多路复用程序 -> 文件事件分派器 -> 事件处理器（命令请求、命令回复、连接应答...）

queue(ss0, s1, s2, ...., sn) -> dispatcher
```

##### I/O 多路复用

I/O 多路复用的功能主要是通过 select、epoll、evport 和 kqueue 这些 I/O 多路复用库实现，在编译 Redis 程序时，会使用系统支持
性能最高的 I/O 多路复用模型 evport -> epoll -> kqueue -> select

I/O 多路复用可以监听的 socket 事件主要分为：
* AE_READABLE 事件：当 socket 可读时(client -> write()，close())，或者有新的 acceptable socket 出现(client -> connect())
* AE_WRITEABLE 事件：当 socket 可写时 (client -> read())

tips: 如果 socket 同时出现 AE_READABLE & AE_WRITEABLE，先处理读事件，在处理写事件。


##### 文件事件的处理器

多个文件事件处理器分别处理不同的网络通信需求：

* 连接应答处理器
* 命令请求处理器
* 命令回复处理器