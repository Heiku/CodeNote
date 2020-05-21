
### Log

#### WAL

WAL（Write-Ahead Logging）：先写日志再写磁盘。

当一条记录需要更新时，InnoDB（独有） 会先把记录写到 redo log 中，并更新内存，等到系统空闲得时候，再将这个操作同步到磁盘中。
保证了数据库得 Crash Safe（在系统崩溃重启之后，能根据 redo日志进行回放，保持原来的状态）。

#### bin log & redo log

1. redo log 是 InnoDb 特有得，bin log 是 Mysql Server 层实现的，所有的引擎都可以使用。
2. redo log 是物理日志，记录的是 “再某个数据页上做了什么修改”，binlog 是逻辑日志，记录的是这个语句的原始逻辑，比如 
“给 ID=2 这行的c字段加 1”

Redo log 记录了这个页“做了什么改动”（改变向量，包括变更的数据块的版本号，事务操作代码，变更从属块的地址以及更新后的数据），
binlog有两种模式，statement 模式记录变更的 sql 语句，row 格式会记录行内容，记录两条（更新前后）。

3. redo log 是循环写，空间固定会用完（通过 write ps 和 checkpoint 双指针推动），binlog是可以追加写入，当 binlog 文件
写入到一定的大小之后会切换到下一个，并不会覆盖以前的日志。

![](/img/mysql-update-write-redo-bin.png)

prepare -> binlog -> commit

如果在写入 binlog 时失败，在重启之后根据 redo log 回放之后，发现数据不一致，回滚操作。
如果在 commit 时失败，因为 redo log 和 bin log 的数据一致，所以可以直接自动提交。


#### 事务

* 读未提交：一个事务还没有提交时，它做的变更就能被其他事务看到
* 读提交：一个事务提交之后，它做的变更才会被其他事务看到
* 可重复读：一个事务执行中看到的数据总是和事务一开始看到的数据是一致的
* 串行化：对同一行记录，写回加写锁，读会加读锁，当出现读写锁冲突的时候，后面访问的事务必须等前一个事务执行完成，
才能继续执行

如果开启事务之后，Mysql 会对行记录维护回滚段，比如（B = B + 1, 那么将会维护 B - 1 的回滚段），在长事务中，可能会存在
很老的事务视图，因为当前事务有可能随时访问数据库的任意数据，在事务提交之前，数据库里它可能用到的回滚记录都必须保留，
这就导致占用大量存储空间。

```
set autocommit = 0，意味着不会自动提交事务，除非自己 commit 或者 rollback，导致长事务

可以使用 commit work and chain 自启动下一个事务，省去 begin 语句开销并且避免了长事务。

可以在 information_schema.innodb_trx 中查询尝试五。
select * from information_schema.innodb_trx where TIME_TO_SEC(timediff(now(),trx_started))>60
```

事务开始：事务开始的时候，会启动一个视图 read-view。

InnoDB 在实现 MVCC 时用到了 一致性读视图，即 Consistent read view，用于支持 RC(Read Committed)和 RR(Repeatable Read)
隔离级别的实现。

```
begin/start transaction 并不是一个事务的起点，而是执行的第一条语句（select/update/....）

start transaction with consistent snapshot  可以马上启动一一个事务
```

InnoDb 里面每个事务都会有一个唯一的事务id，transaction id。在事务开始的时候向 InnoDb 的事务系统申请的，按申请顺序严格递增的。

每行数据是有多个版本的，每次事务更新数据的时候，都会生成一个新的数据版本，并且把 transaction id 赋值给这个数据版本的事务，
记为 row trx_id。同时，旧的数据版本也会保留，并且在新的版本中，能够有信息可以直接拿到它。

![](/img/mysql-transaction-snaphot.png)

undo log 根据图中的 U3 U2 U1 计算。

##### 事务视图

InnoDb 为每个事务构造了一个数组，用来保存这个事务启动瞬间，当前正在 ”活跃“ 的所有事务ID，”活跃“ 指的是启动但未提交。

数组里面事务ID 得最小值记为低水位，当前系统里面已经创建过得事务 ID 的最大值加1为高水位。

![](/img/mysql-transaction-water-level.png)



### 锁

##### 全局锁

全局锁对整个数据块实例加锁，整个库处于只读状态 `Flush tables with read lock`，其他线程的增删改、建表、事务提交等操作
都将被阻塞。可用于全库的逻辑备份。

##### 表级锁

1. 表锁

`lock table ... read/write`，例如 `lock tables t1 read, t2 write`，那么只能对 t1 读，t2 读写，无法访问其他表。

2. MDL(Metadata Lock)

MDL 保证了在表结构被修改的时候，读写的正确性。当对一个表做 CRUD 的时候，加上 MDL 读锁；当对表结构做变更操作的时候，
加 MDL 写锁。

* 读锁之间不互斥，因此可以有多个线程同时对一张表增删改查
* 读写锁、写锁之间是互斥的，用来保证变更结构操作的安全性。因此如果有两个线程要同时对同一个表加字段，其中一个要等到
另一个执行完才能开始执行。

```
sessionA select  读锁
sessionB select  继续加读锁
sessionc alter field    修改字段加写锁，与读锁互斥，必须等待读锁释放
sessionD select  加读锁失败，必须等待写锁释放

如果读请求得客户端有重试机制，直接新建session发起读请求，这样会一致僵持，到时线程资源占用。

可以采用加上超时时间得方式尝试添加字段
alter table T wait/nowait add column
```

###### 行锁

```
A           B
update 1
update 2
            update 1
commit
            commit

B事务的 update 会被阻塞，直到事务 A 执行 commit 之后才会释放 1 的行锁
```

在 InnoDB 事务中，行锁是需要的时候才加上的，但并不是不需要了就立刻释放，而是等到事务结束时才释放。__所以在事务中，
如果事务中需要锁住多个行，要把可能造成锁冲突、最可能影响并发度的锁往后放__。

###### 死锁

```
A           B
update1
            update2
update2
            update1
commit
            commit
```

* innodb_lock_wait_timeout

默认为50s，最好不要修改，如果修改的太小，有可能因为实际操作的耗时较长导致的误伤。

* 死锁检测

主动发起死锁检测，发现死锁之后，主动回滚死锁链条中的某一个事务，让其他事务能够执行。 `innodb_deadlock_detect=on`默认开启。


```
delete 1w

1. delete * from T limit 100000;    // 一次锁住的行太多，其他客户端等待时间长
2. 一个连接循环20次 limit 5000         // 分为多次短事务，每次事务占用的时间较短，每次执行使用不同分段的资源，提高并发量
3. 20个连接 limit                  // 自己制造锁竞争，并发量下降
```

