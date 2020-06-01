
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

对于事务启动瞬间，一个数据版本的 row trx_id 有以下几种可能：

1. 如果落在绿色部分，表示这个版本是已提交的事务或者是当前事务自己生成的，数据可见。
2. 如果落在红色部分，表示这个版本是将来启动的事务生成的，不可见。
3. 如果落在黄色的部分，包括两种情况：
    a. 若 row trx_id 在数组中，表示这个版本是还没提交的事务生成的，不可见
    b. 若 row trx_id 不在数组中，表示这个版本是已经提交了的事务生成的，可见

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

### 索引

#### 普通索引 和 唯一索引

`select id from T where c = 5`，通过索引树，从树根开始按层搜索到叶子，在叶子节点的数据页内通过二分定位记录。

* 普通索引：查找到满足 c=5 之后，还是会继续查找直到碰到第一个不满足 c=5 的记录
* 唯一索引：查找到第一个满足条件的记录之后，停止检索

##### change buffer

当需要更新一个数据页时，如果数据页在内存中，直接对数据页进行修改，如果数据页不在内存中，在不影响数据一致性的前提下，
InnoDb 会将这些更新操作缓存在 change buffer 中，这样就不用在磁盘中读取这个数据页。在下次查询需要访问这个数据页的时候，
__将数据页读入内存，再执行 change buffer 中与这个页有关的操作__。

将 change buffer 中的操作应用到原数据页，得到最新结果的过程称为 merge。除了访问这个数据页会触发 merge 外，系统有
后台线程会定期 merge，在数据库正常关闭（shutdown）的过程，也会执行 merge 操作。

当需要更新的数据页不在内存的时候：
* 唯一索引：需要直接直接加载数据页到内存上，判断是否已有相同的记录，没冲突再插入
* 普通索引：并不关心是否重复，将更新记录到 change buffer 上。减少了磁盘 I/O 带来的随机访问。

##### change buffer 使用场景

change buffer 的主要目的是将记录的变更动作缓存下来，所以在一个数据页做 merge 之前，change buffer 记录的变更越多，
收益越大。所以对于写多读少的业务来说，使用 change buffer 的效果最好，例如日志系统、账单系统。

但如果一个业务的更新模式是写入之后马上做查询，会立即访问数据页，在 change buffer 记录少的情况下触发 merge 过程，
并不会减少随机访问 I/O 的次数，而且还需要维护 change buffer，性能下降。

所以如果是更新之后立即查询的话，应该关闭 change buffer，但这种情况比较少，开启之后还是能提升更新性能。

* redo log/change buffer

![](/img/mysql-change-buffer-redo-log.png)

redo log 主要是记录了 __页的变化（WAL 将页变化的乱序写转换成了顺序写）__。redo 日志有几十种类型，包括了B+索引页，
undo页，以及 change buffer 页等等。change buffer 写入到 redo log 之后，可以避免立即刷盘。但 change buffer 是需要
持久化的，它的持久化工作和其他页一样，交给了 redo 日志完成。所以如果 change buffer 写入redo 日志崩溃，可以通过 redo 日志
恢复 change buffer。

change buffer 持久化文件为 idbdata1.  
索引页的持久化文件为 t.ibd.

#### Mysql 选错索引

```
analyze table t;    重新痛惜索引信息（索引基数）

select * from table force(a) where a between 100 and 200    索引修正
```


#### 字符串索引

前缀索引: alter table T add index(email(6))，可以减少索引占用的空间，但是可能会存在多条记录回表，精确度不够。
倒叙索引：select a from T where a = reverse('')
hash索引：alter table t add id_card_crc int unsigned, add index(id_card_crc);  精确度高，但需要额外空间存储 hash 值，
同时计算的过程中会消耗更多的 CPU。


#### 抖动

InnoDb 中的更新语句都是会根据 WAL 先写入 redo log，等待实际再更新磁盘。再将脏页刷入磁盘的时候，就有可能产生”抖动“。

抖动产生的例子:  
1. 一个查询要淘汰的脏页个数太多，会导致查询的响应时间明显变长
2. 日志写满，更新全部堵住，写性能跌为0


脏页：内存数据页与粗盘数据页内容不一致。
干净页：内存数据写入到磁盘后，内存和磁盘的数据页内容一致。

1. redo log 写满了
2. 系统内存不足，当需要新的内存页时，就需要淘汰”脏页“，将脏页写入到磁盘上。
3. 系统空闲的时候，在适当的时候写入磁盘
4. Mysql 正常关闭，将内存的脏页 flush 磁盘上。


```
innodb_io_capacity 和 关注脏页的占用比例

select VARIABLE_VALUE into @a from global_status where VARIABLE_NAME = 'Innodb_buffer_pool_pages_dirty';
select VARIABLE_VALUE into @b from global_status where VARIABLE_NAME = 'Innodb_buffer_pool_pages_total';
select @a/@b;
```

#### 数据删除，表文件不变

innodb_file_per_table

1. OFF: 表的数据放在系统共享表空间，与数据字典放在一起
2. ON: 每一个 InnoDb 表数据存储在一个 .idb 文件中, 推荐使用

* 表删除

在删除数据时，只是将对应页上的数据行 __标记__ 为删除，如果下次在这个位置上插入数据时，可以直接复用，
当然如果两个数据页的利用率很低，那么系统会合并这两个数据页。所以如果我们 delete 数据，只是将数据页
标记为 “已删除”，磁盘文件大小无变化。

可以使用 `alter table T engine=InnoDB` 重建表，本质上是新建表迁移数据并更新表名。但这个过程原表A的新记录可没有
写入到新表，所以可以使用 Online DDL 的方式，类似于 Redis 的主从，是先进行快照将数据迁移，然后对 A 的更新都记录
到日志文件（row log）中，最后将日志文件中的记录写入到临时文件B中。

区分一下：  
1. alter table T engine = InnoDB: 重建表，减少页空间的浪费
2. analyze table T: 对表的索引信息进行重新统计（用于处理索引数据错误的文图），并不修改数据，加 MDL 读锁
3. optimize table: recreate + analyze


#### count(*)

Engine: 
1. MyISAM 会把一个表的总行数（not where）存储在磁盘上，执行 count(*) 的时候可以直接返回
2. InnoDb 则需要把数据一行一行地从引擎中读出来，然后累积计数。

InnoDb 的优化：  
普通索引树比主健索引树小很多，对于 count(*) 遍历哪种索引树的结果都是一样的，所以在保证逻辑正常的情况下，
尽量减少扫描的数据量。注意：show table status 中的 TABLE_ROWS 存在误差。

区分：  
1. count(id): 遍历主键表，然后取id，返回给 server 层，server判断如果不为空+1
2. count(1): 遍历整张表，但不取值，server放入数字”1“，判断不为空之后，进行累加，
相比于count(id)不解析数据行，草被字段值，更快一些
3. count(字段): 类似 count(id)，取出每一行的字段，然后判断是否为空，不为空+1
4. count(*): 额外优化，不取字段，只计算数据行，挑占用空间更少的索引树进行计算。

count(字段) < count(id) < count(1) <= count(*)

#### 日志索引问题

![](/img/mysql-innodb-redo-log-bin-log-process.png)

如果在 redo log 写入后失败，那么系统恢复之后进行事务回滚。  
如果在 bin log 写入后，在 commit 之前失败，那么会有以下情况：  
1. 如果 redo log 中事务完整（commit），那么直接提交
2. 如果 redo log 中只有 prepare，则判断对应事务 binlog 是否存在并完整：如果bin log 完整提交，不完整则回滚。

* bin log 完整性？

statement 格式中bin log有 COMMIT，row 格式中，最后有 XID event

* redo log / bin log 如何关联？

两者有公共数据字段 XID, 当系统崩溃时，会按照顺序扫描 redo log，如果既有 prepare 又有 commit，直接提交。否则再查 bin log。

* 为什么需要先 redo log，再 bin log？

为了保证主从的一致性，如果写完 bin log 之后崩溃，这时bin log 已经写入，之后就会被从库读取。

* 是否可以直接使用 binlog? 

历史遗留问题，bin log 本身并不支持 crash safe，崩溃恢复的功能，还是需要 Innodb 自身的 redo log 提供支持。
反之，也不能只用 redo log，因为本身时循环日志写，历史日志无法保留，无法起到归档的作用。同时Mysql server 层的
主从同步复制主要还是通过 bin log 实现。

#### Order

Order By 通常是使用索引得到的数据后回表查找select 对应的数据行，然后再内存中 sort_buffer 中进行快速排序，
如果数据量太大内存放不下的情况，就需要使用临时文件排序，将部分数据加载到内存然后最终在多个临时文件中进行归并排序，
最终形成一个有序的临时文件。

`max_length_for_sort_data` 如果单行的长度太大（字段多且大），加载到内存排序的时候无法一次性完成，会通过临时文件。
这时会使用 rowId 排序，对内存中 sort_buffer 的字段主键映射（name-id）排序后，取主键进行回表，最终得到数据行。
整个过程多一次回表。

可以利用 联合索引 本身有序的方式减少排序带来的性能消耗。(city-name)

#### SQL 性能差异

1. 条件字段函数操作

```
select count(*) from tradelog where month(t_modified)=7
```

2. 隐式类型转换

```
id(varchar) select * from table T where id = 2;

select * from table T where CAST(traid as signed int) = 2;
优化器会对字段进行函数转换，放弃树的搜索功能，导致全表扫描
```

3. 隐式字符编码转换

```
多表连接的时候如果字符集不同，会使用编码转换函数修改索引值，导致索引失效。
select * from trade_detail where CONVERT(traideid USING utf8mb4)=$L2.tradeid.value;
```

#### join

优化器会自动选择 __小表驱动大表__，减少复杂度和磁盘IO

#### 查询一行时间长

1. 等待 MetaDataLock

```
lock tables t write;
show processlist;
unlock tables

select blocking_pid where sys.schema_table_lock_waits;
```
会使其他的 select 进行等待锁阶段，可以将阻塞的 pid kill 掉。

2. 等待 flush

```
select * from information_schema.processlist where id=1; (waiting for table flush)

flush tables t with read lock;
flush tables with read lock;
关闭表
```

3. 等待锁

之前的例子都是表级别锁，而等待锁进入到了执行引擎中。

```
    A                               B
    begin
    update c = c + 1 where id = 1;
                                    select * from t where id = 1 lock in share mode;

A 事务如果一直不提交，B 将一直阻塞在等待锁状态。
```

可以通过 sys.innodb_lock_waits 表查到具体信息，及时 kill 对应的pid，释放锁


```
A                               B
start transaction with consistent snapshot;
                                update t set c = c+1 where id = 1;(加锁执行100w次)
select * from t where id = 1;
select * from t where id = 1 lock in share mode;

lock in share mode，为当前读，所以可以直接读到执行后的结果 1000001，
select * from; 快照读，所以需要从 1000001 开始，依次执行 undo log，才返回事务开始的数据 1。
```

#### 幻读

```
begin;
select * from t where id = 1 for update;    
commit;

select 语句执行完成之后，会在这一行加一个写锁，由于两段锁协议，写锁将会在执行 commit 的时候释放。
```

幻读：行锁只能锁住行，但是新插入记录的动作，要更新的是记录之间的“间隙”，所以需要“间隙锁”-GAP LOCK。

```
0 5 10 15 20

(-∞,0) (0,5) (5,10) (10,15) (15,20) (20,+∞)
```

间隙锁和行锁合称为 next-key lock，每个 next-key lock 都是前开后闭区间。(-∞,0],(0,5]...  
间隙锁的引入，可能会导致同样的语句锁住更大的范围，影响了并发度，避免可以采用读提交的隔离界别，但需要把
binlog 格式设置成 row，避免数据与日志不一致的情况。


#### 改一行，锁多行

加锁规则：

1. 原则 1：加锁的基本单位为 next-key lock，前开后闭
2. 原则 2：查找过程中访问到的对象才会加锁
3. 优化 1：索引上的等值查询，给 __唯一索引__ 加锁的时候，next-key lock 退化成行锁
4. 优化 2：索引上的等值查询，向右遍历时且 __最后一个值__ 不满足等值条件的时候，next-key lock 退化成间隙锁。
5. 唯一索引上的范围查询会访问到不满足条件的第一个值为止


* 等值查询间隙锁

```
A               B               C
begin
update t set d=d+1 where id = 7;
                insert into t values (8,8,8)
                (blocked)
                                update t set d=d+1 where id=10;
                                (query ok)

(5,10]，next-key lock，所以session B 失败，因为第7行没有可以锁住的，优化2退化间隙锁，所以 session C 正常获得锁并更新
```

* 非唯一索引等值锁

```
A               B               C
begin;
select id from t where c = 5 lock in share mode;
                update t set d=d+1 where id = 5;
                (Query ok)
                                insert into t values (7,7,7)
                                (blocked)

(0,5] 因为非唯一索引，可能会有重复值，所以会继续查直到在区间 (5,10]，因为最后一个值无法等值匹配，所以退化成 (5,10)
即锁住的区间为 (0,5] (5,10)

session A 给索引 c=5 加上读锁

只有访问的对象才会加锁，这个查询使用覆盖索引，并不需要访问主键，所以主键索引上没有加任何锁，
所以session B 的语句可以执行完成（只使用了索引c，返回的数据也不用回表，锁只加在索引c上）

如果是 for update，系统会认为接下来要更新数据，顺便给主键索引上满足条件的行加上行锁，即锁住实际的数据行，
这时 session B 将被 blocked（或者 select d from t where c=5 lock in share mode）
```

* 主键索引范围锁

```
A               B               C
begin;
select * from t where id >= 10 and id < 11 for update;

                insert into t values(8,8,8);
                (Query OK)

                insert into t values (13,13,13);
                (Blocked)
                                update t set d=d+1 where id = 15
                                (Blocked)

session A 要找到id=10 的行，(5,10] 因为等值匹配，所以优化一退化行锁，所以只锁住 id=10 的行，
因为范围索引 < 11，所以到区间会寻找下一个间隙锁 (10,15] 所以 插入 8 的时候正常，插入 13 的时候被阻塞，

session A 这时候锁住的范围为主键索引上，行锁 id=10，和 next-key lock (10,15]
```

* 非唯一索引范围锁

```
A               B               C
begin;
select * from t where c>=10 and c<11 for update;
                insert into t values(8,8,8)
                (blocked)
                                update t set d=d+1 where c=15;
                                (blocked)
```

非唯一索引，不退化成行锁，所以锁住的范围为 (5,10],(10,15]，

##### 性能提升

* 短连接

短连接模型一旦数据块处理得慢，连接数就会暴涨，max_connections 如果超出，会报错 "too many connections"，
数据库这时对对外服务不可用，

如果连接数过多，优先断开事务外空闲太久的连接；如果还不能缓解，在考虑断开事务内空闲太久的连接。
`kill connnection + id`

* 慢查询

1. 索引没设计好

通过紧急加索引解决，在 MySQL 5.6 之后，创建索引支持 Online DDL，可以直接执行 alter table。  
如果服务器是主从备份，建议先在备库中执行 `set sql_log_bin=off`，不写 binlog，然后执行 `alter table ... `
创建索引，接着执行主从切换，这时候原来的主库需要重复执行一次这样的操作 `close binlog and alter table`

建议使用 [gh-ost](https://github.com/github/gh-ost)

2. SQL 语句没写好

是否收到函数的影响，值转换等问题，可以通过 query_write 判断之前的语句是否正确。

3. Mysql 选错了索引

mysql 自己选错了索引，可以analy以下，如果没有效果，可以强制使用 force index。最好是上线前进行测试：  
打开慢日志，并设置 long_query_time = 0，确保每个语句都会被记录，然后插入模拟数据进行回归测试，
观察每个语句的 rows_examined 是否与预期一致。


##### 如何保证数据不丢失

##### binlog 写入机制

事务执行的过程中，先把日志写到 binlog cache，事务提交的时候，再把 binlog cache 写到 binlog 文件中。

系统会为 binlog 分配一片内存，每个线程一个，参数 binlog_cache_size 用于控制单个线程内 binlog cache 
所占的内存大小，如果超过这个参数规定的大小，就要暂存到磁盘。

![](/img/mysql_binlog_write.png)

* write: 指的是将日志记录写道文件系统的 page cache，并没有持久化到磁盘，所以速度比较快
* fsync: 将数据持久化到磁盘中，fsync 会占用磁盘的 IOPS

write/fsync 的时机由参数 sync_binlog 控制：

1. sync_binlog = 0，每次提交事务都只 write，不 fsync
2. sync_binlog = 1，每次提交事务都会 fsync
3. sync_binlog = N(N > 1)，表示每次提交事务都 write，但累积 N 个事务后才 fsync。能提升性能，但存在数据丢失问题。
（100-1000）

##### redo log 写入机制

在事务执行的过程中，生成的 redo log 要先写到 redo log buffer 中，所以redo log 在写入的过程中，可能存在以下几种
状态：

![](/img/mysql-redo-log-redo-log-buffer.png)

1. 存在 redo log buffer 中，物理上在 MySQL 进程内存中，red
2. 写道磁盘（write），但是没有持久化（fsync），物理上是在文件系统的 page cache 里面，yellow
3. 持久化到磁盘，对应的是 hard disk，green

redo log 的写入策略由参数 `innodb_flush_log_at_trx_commit` 参数控制

1. 0: 事务提交的时候只是把 redo log 留在 redo log buffer 中
2. 1: 事务提交的时候都将 redo log 持久化到磁盘上
3. 2: 事务提交的时候把 redo log 写到 page cache 上

InnoDB 有一个后台线程，每隔1s，把 redo log buffer 中的日志调用 write 写到文件系统的 page cache，
然后调用 fsync 持久到磁盘上。（因为后台会不定期写到磁盘，所以没提交的事务的 redo log 也可能被持久化
到磁盘上）


