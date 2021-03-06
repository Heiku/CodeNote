
#### DDL DML

DML(Data manipulation language): select、insert、update、delete  
DDL(Data definition language): create、alter、delete



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




##### 为什么需要 redo log？

InnoDb 是以页来管理存储空间，在访问页面之前，我们需要将把在磁盘上的页缓存到内存中的 `buffer pool` 中才可以访问，
在事务中，为了保证持久性，对于一个已提交的事务，在事务提交后即使系统发生了崩溃，事务对数据库所作的更改也不能丢失。
简单的做法就是：__在事务提交完成之前把该事务所修改的所有页面刷新到磁盘上__，但存在以下两个问题：

* 刷新一个完整的数据页太浪费了

有时候我们只是修改了某个页面中的一个字节，但是 InnoDb 是以 `页` 为单位来进行磁盘IO，也就是说事务提交的时候，
不得不将一个完整的页面从内存中刷新到磁盘，如果只修改一字节而刷新16K的数据页太浪费了。

* 随机IO效率低

一个事务中可能包含多个语句，会修改多个页面，而页面与页面之间并不相邻，意味着将某个事务修改的 `buffer pool` 中
的页面刷新到磁盘时，需要进行很多随机IO，随机IO比顺序IO慢很多。



#### 事务

* 读未提交：一个事务还没有提交时，它做的变更就能被其他事务看到

* 读提交：一个事务提交之后，它做的变更才会被其他事务看到

* 可重复读：一个事务执行中看到的数据总是和事务一开始看到的数据是一致的

* 串行化：对同一行记录，写回加写锁，读会加读锁，当出现读写锁冲突的时候，后面访问的事务必须等前一个事务执行完成，
  才能继续执行

  

##### 事务状态

* 活动的(active): 事务对应数据库正在执行的过程

* 部分提交(partially committed): 事务最后一个操作执行完成，但操作都在内存中，所造成的影响并没有刷新到磁盘上

* 失败(failed): 当事务处于 `活动的` 或者 `部分提交的`，可能遇到了某些错误（数据库自身的错误、操作系统错误或断电等）
导致无法继续执行，或者认为的停止当前事务的执行

* 中止(aborted): 如果事务执行了半截变成 `失败的` 状态，就需要撤销失败事务对当前数据库的影响，这个撤销的过程称为 `回滚`。
当 `回滚` 操作执行完毕时，也就是数据库恢复到执行事务之前的状态，我们称事务处于 `中止` 状态。

* 提交(committed): 当一个事务处于 `部分提交的` 状态的事务将修改过的数据同步到磁盘上

  

![](/img/mysql_transaction_status.png)

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
begin/start transaction（start transaction READ ONLY /  READ WRITE 指定读写事务） 并不是一个事务的起点，而是执行的第一条语句（select/update/....）

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
    
    

##### ReadView

每条记录在更新之后，都会将旧值放到 `undo 日志` 中，作为该记录的一个旧版本（事务提交移除），随着更新的次数增多，所有
的版本都会被行记录的 `roll_pointer` 属性连接成一个链表，称为 `版本链`，版本链的头节点就是当前记录的最新值。另外，每个版本
还包含生成该版本的 `事务id`。

![](/img/mysql-mvcc-readview-undo-record-list.png)

`READ_COMMITED`、`REPEATABLE READ` 这两个隔离级别的很大不同在于：生成 `ReadView` 的时机不同，`READ_COMMITED` 在每一次
进行普通 select 操作前都会生成一个 ReadView，而 `REAPETABLE_READ` 只在一次进行普通 select 操作前生成一个 ReadView，
之后的查询操作都重复使用这个 ReadView。

为了支持MVCC，对于delete mark操作来说，仅仅是在记录上打一个删除标记，并没有真正将它删除掉  



### 锁



##### 全局锁

全局锁对整个数据块实例加锁，整个库处于只读状态 `Flush tables with read lock`，其他线程的增删改、建表、事务提交等操作
都将被阻塞。可用于全库的逻辑备份。

##### 表级锁

1. 表锁

`lock table ... read/write`，例如 `lock tables t1 read, t2 write`，那么只能对 t1 读，t2 读写，无法访问其他表。

2. MDL(Metadata Lock)

MDL 保证了在 __表结构__ 被修改的时候，读写的正确性。当对一个表做 CRUD 的时候，加上 MDL 读锁；当对表结构做变更操作的时候，
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

redo log 主要节省的是随机写磁盘的 IO 消耗（转成顺序写），而 change buffer 主要节省的则是随机读磁盘的 IO 消耗



#### Mysql 选错索引

```
analyze table t;    重新分析索引信息（索引基数）

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
2. analyze table T: 对表的索引信息进行重新统计（用于处理索引数据错误的情况），并不修改数据，加 MDL 读锁
3. optimize table: recreate + analyze


online ddl 使用 inplace 模式，是可以避免 MDL 写锁阻塞读锁（中间会降级，避免影响正常 DML 语句），但从库执行同步过来的 DDL 语句的时候，
会阻塞进行中的 DML（MDL 写锁），所以会放大主从不一致的情况。

[Mysql Online DDL](https://juejin.cn/post/6854573213167386637)  
[Mysql Online DDL 过程](https://cloud.tencent.com/developer/article/1574126)

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
相比于count(id)不解析数据行，不读取字段值，更快一些
3. count(字段): 类似 count(id)，取出每一行的字段，然后判断是否为空，不为空+1
4. count(*): 额外优化，不取字段，只计算数据行，挑占用空间更少的索引树进行计算。

count(字段) < count(id) < count(1) <= count(*)

count(字段) <= count(id)，是因为如果该字段没建立索引，那么就有直接走主键索引，解析得是数据量比较大。
而 count(id) 会优先选取数据量小得索引进行计数。



#### 日志索引问题

![](/img/mysql-innodb-redo-log-bin-log-process.png)

* 过程 (update t set a = a + 1)
  
    1. 先记录 undo log a (依托于 redo log)
    2. 修改对应页得数据行 (等待系统刷盘)
    3. 然后记录对应页上的修改到 redo log (我们熟知的 redo log 概念，移动 lsn balabala...)，进入 __PREPARE__ 状态
    4. 接着写下逻辑日志到 bin log (记录数据库中的变化，用于同步(注意区分一下索引数据文件))，这期间都有可能因为其他事务提交使得 redo log/bin log 都 `fsync` 写入磁盘文件中
    5. `commit` 的时候（双一配置, `innodb_flush_log_at_trx_commit` `sync_binlog` ）将两个修改完的处于 `pageCache` （maybe）中的日志数据写入到磁盘中，进入 `COMMIT` 状态
    



##### 分析

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



#### 性能提升

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

mysql 自己选错了索引，可以analyze以下，如果没有效果，可以强制使用 force index。最好是上线前进行测试：  
打开慢日志，并设置 long_query_time = 0，确保每个语句都会被记录，然后插入模拟数据进行回归测试，
观察每个语句的 rows_examined 是否与预期一致。



#### 如何保证数据不丢失

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

* lsn

当有新的 `redo` 日志写入到 `log buffer` 时，首先 `lsn` 的值会增长，但 `flushed_to_disk_lsn` 不变，随后随着不断有 
`log_buffer` 中的日志被刷新到磁盘上，`flushed_to_disk_lsn` 的值也会跟着增长。如果两者的值相同，说明 `log buffer` 
中的所有 redo 日志已经刷新到磁盘中（未fsync）。同时还有一个 `checkpoint_lsn` 是为了循环使用 redo 日志，在 `checkpoint_lsn` 
之前的日志都可以被覆盖，因为已经刷新到磁盘中。

在事务执行的过程中，生成的 redo log 要先写到 redo log buffer 中，所以redo log 在写入的过程中，可能存在以下几种
状态：

![](/img/mysql-redo-log-redo-log-buffer.png)

1. 存在 redo log buffer 中，物理上在 MySQL 进程内存中，red
2. 写道磁盘（write），但是没有持久化（fsync），物理上是在文件系统的 page cache 里面，yellow
3. 持久化到磁盘，对应的是 hard disk，green

redo log 的写入策略由参数 `innodb_flush_log_at_trx_commit` 参数控制

1. 0: 事务提交的时候只是把 redo log 留在 redo log buffer 中，交由后台线程写入磁盘
2. 1: 事务提交的时候都将 redo log 持久化到磁盘上，可以保证事务的 `持久性`
3. 2: 事务提交的时候把 redo log 写到 page cache 上，交由操作系统决定写盘

InnoDB 有一个后台线程，每隔1s，把 redo log buffer 中的日志调用 write 写到文件系统的 page cache，
然后调用 fsync 持久到磁盘上。（因为后台会不定期写到磁盘，所以没提交的事务的 redo log 也可能被持久化
到磁盘上）

"双1"配置：`sync_binlog` 和 `innodb_flush_log_at_trx_commit` 都设置成1，一个事务完整提交前，
需要等待两次刷盘，一次 redo log，一次 binlog。



##### redo log 恢复数据


##### LSN

日志逻辑序列号（Log Sequence Number）: 对应每次redo log 的写入点，每次写入 length 时，LSN + Length

group commit: 为了提升刷盘的吞吐量，在并发的多个事务中，多个事务（形成一个组）会更新 LSN 值，在写盘的时候，会带最新的 LSN，
即一次将整个组的事务 redo log 记录写入磁盘中，节约磁盘的 IOPS。

![](/img/mysql-redo-log-group-commit.png)

##### WAL

1. redo log 和 binlog 都是顺序写，磁盘的顺序写比磁盘的随机写快很多
2. 组提交机制，可以大幅度降低磁盘的的 IOPS 消耗

* 如果 Mysql 出现性能瓶颈，瓶颈在 IO 上，如何优化呢？

1. 设置 `binlog_group_commit_sync_delay`（延时多久调用 fsync） 和 `binlog_group_commit_sync_no_delay_count`
（累计多少次组提交才调用 fsync）参数，减少 binlog 的写盘次数。（基于“额外的等待时间”，可能会增加语句的响应时间）
2. `sync_binlog` 设置大于1（100-1000），断电会丢数据
3. `innodb_flush_log_at_trx_commit` 设置为2，断电丢数据



#### 主备一致

* 事务日志同步过程(A -> B)

1. 备库B 通过 `change master` 设置主库A 的IP、端口、用户名、密码等（构建连接），以及从哪个位置开始请求binlog，
这个位置包含了文件名和日志偏移量。
2. 备库B 执行 `start slave` 命令，备库这时会启动两个线程，io_thread 和 sql_thread(多线程)。
3. 主库A 校验完连接信息后，按照备库B 传过来的位置，开始读取本地 binlog，发给B
4. 备库B 拿到 binlog 后，写道本地文件，称为中转日志（relay log）
5. sql_thread 读取中转日志，解析日志中的命令，并执行。



##### binlog 格式

```
查看 binlog 日志

show master logs;       // 查询所有 binlog 日志
show master status;     // 查看最后一个事件记录的日志文件及偏移量
show binlog events in 'master.000001';      // 查看 binlog
```

* __statement__：记录了真实执行的数据行

1. `SET @@SESSION.GTID_NEXT= 'ANONYMOUS'`
2. `BEGIN`, 表示一个事务
3. `use test``deltet * from t where ...`, use 为自动添加，保证日志传到备库执行的时候，无论当前工作线程在哪个库中，
都能正确到执行库中更新语句。
4. `COMMIT`

缺点：在 __statement__ 格式下，记录到 binlog 的语句是原文，可能会出现在执行某条 SQL 语句的时候，
用的是索引 a，而备库在执行这条语句的时候，却使用了索引 b，写操作存在风险。

* __row__: 使用了事件标识代替了原来的执行语句（具体可以通过 `mysqlbinlog -vv data/master.0001 --start-position=8900` 解析）

1. ...
2. `Table_map event`: 表示操作的表
3. `Delete_rows event`: 定义删除的行为
4. `commit`

使用 row 格式时，binlog 记录了真实数据行的主键id，当binlog 传入到备库的时候，会直接更新对应数据行的记录，不会存在 
statement 的问题。

* mixed

1. statement 格式的 binlog 可能会导致主备不一致，所以需要使用 row 格式
2. row 格式会占用大量的空间。比如一个 `delete * from limit 10w`，statement 只会记录这条 sql 语句中，占用几十个字节空间，
如果使用的是 row 格式，需要把这 10w 条记录写到 binlog 中，会占用大量的空间，同时写 binlog 也会耗费 IO 资源，影响执行速度。
3. mixed 是一个折中的方案，MySQL 会自动判断这条语句是否会引起主备不一致，如果可能的话，使用 row 格式，否则使用 statement 格式。

```
insert into t values(1, now())
这个语句主库会在 binlog 记录 event 的时候，多记一条记录：SET TIMESTAMP = 1546103491，
以保证在主备数据的一致，

如果重放 binlog 数据的时候，可以使用 mysqlbinlog 工具解析，再交由 MySQL 执行
mysqlbinlog master.000001 --start-position=2738 --stop-position=2973 | mysql -h127.0.0.1 -P3000 -u$user -p$pwd;
（解析 binlog 中position 2738-2973 的记录，然后放到 Mysql 中执行）
```



#### 保证高可用

备库最好设置成 `readonly` 只读模式：

1. 防止特殊的运营查询需求影响到备库的数据，防止误操作
2. 防止切换逻辑 BUG，例如切换过程中的双写，造成主备不一致
3. 利用 readonly 状态判断节点的角色



##### 主备延迟

* 同步延迟

1. 主库A 执行一个事务，写入 binlog，T1
2. 之后传给备库B，备库B 接收完这个 binlog，T2
3. 备库B 执行完成这个事务，T3

```
show slave status

seconds_behind_master: 计算与当前系统时间的差值

备库在连接到主库的时候，会通过执行 select UNIX_TIMESTAMP() 函数去获得主库的系统时间，如果不一致，
在计算 seconds_behind_master 会减去差值
```

正常网络情况下，日志传输到备库的时间很短（T2-T1），主要体现在备库消费中转日志（relay log）的速度，
比主库生产 binlog 的速度更慢。



##### 主备延迟来源

1. 在部署的时候，备库所在的机器 __性能__ 远比主库的机器性能差
2. __备库的压力大__，某些业务为了不影响主库上的正常业务，在备库上执行一些比较耗时的查询任务，
导致备库上耗费了大量的 CPU 资源，影响了同步速度，造成延迟加重。

解决办法：可以多接几个从库或者使用 Hadoop 这类系统，让系统外部提供统计查询的能力。

3. __大事务__

如果一个语句在主从执行10分钟，那么从库将会延迟10分钟，例如一次 delete 大量数据，可以分为多次事务删除。  
除此之外，还有大表的 DDL。



##### 可靠优先策略（HA 系统的处理）

* 可靠优先策略：保证同步后再切换，数据一致

1. 判断备库的 SHM(seconds_behind_master)，如果小于某个值（5s）继续下一步，否则持续重试 
2. 把主库 A 改成只读状态，即把 readonly 设置成 true
3. 判断备库的 SHM 值，直至变为 0 即已经完全同步为止
4. 把备库 B 改成读写状态，即 readonly 设置成 false
5. 把业务请求切到备库 B

因为切换过程中 主库A 和备库 B 都是处于 readonly 状态，即系统处于不可用，所以第一步的 __尝试同步__ 要 SHM 尽量小。

* 可用性优先策略：在延迟较大的时候能对外提供读写，但是在双M结构下，同步的时候会出现数据不一致的问题

可以采用 binlog row 的方式，更容易发现数据不一致的问题



#### 备库延时

relay log -> coordinator(sql thread) -> workers... 

coordinator 在分发需要满足两个基本要求：

1. 不能造成更新覆盖。（这要求更新同一行的两个事务，必须被分发到同一个 worker 中）。
2. 同一个事务不能被拆开，必须放到同一个 worker 中



#### 主从问题

##### 主备切换

![](/img/mysql_master_slave_sync_sulotion.png)

```
A(下线)   BCD（备库 readonly）
A`（切换）

B 切换成 A` 的时候，执行 change master(host、port、user、password、binlog_name、binlog_pos)
这里 A` 的 pos 同步位置的获取是通过 A` 把 relay log 完全同步后，通过 show master status 获取自身的最新 pos，
但这个过程仍可能存在备库 BCD 已经同步的情况，即 pos 可能已经被失效，但 BCD 执行同步的时候还是从这个位置获取，
所以需要注意的是如果是插入删除的情况会导致同步的时候报错，例如 `Duplicate entry `id` for primary key` 导致同步停止，
可以手动 `set global sql_salve_skip_counter| salve skip_errors` 跳过报错继续执行同步
```

* GTID

GTID: Global Transaction Identifier 全局事务ID，一个事务提交的时候生成，

```
GTID: server_uuid:gno

server_uuid: server 实例启动时自生成，全局唯一的值
gno: 提交事务的时候分配配的id，自增+1

gno 区分 transaction_id，事务id如果回滚，事务id会递增，而gno只会在事务提交的时候分配
```

主备切换:

```
change master to
... 
master_auto_position=1  // 主备使用 GTID 协议,不需要指定 log_name & log_position

上面的例子，如果这时候 A 的部分事务已经同步到了 B，那么A` 和 B 都会有相同的 GTID
在 B 与主库 A` 建立连接的时候，会将自己的所有 GTID 集合发送给主库 A`，
然后A`再将自身的 GTID 集合与之取差集，然后从不重复的事务开始取 pos 发送给 备库 B，
从这个位置开始的都将不会重复。
```



#### 读写问题

![](/img/mysql_master_slave_read_write_proxy.png)

* 直连 vs proxy 优缺点：

1. 客户端直连，少了 proxy 转发性能更好，架构简单，排查会比较方便。但因为后端部署细节，所以在
主备切换、库迁移等操作客户端都会感知到，需要及时调整数据库连接信息。
2. proxy 代理，客户端并不需要知道部署细节，连接维护、后端信息维护等工作都是由 proxy 完成，
同时要求 proxy 要具备高可用架构，相对复杂。



##### 过期读

不管哪种方式，由于主从存在延迟，客户端执行一个更新事务后查询，如果查询选择的是从库，就有可能
存在读取的记录是更新前的状态。

* 强制走主库

1. 对于那些更新后需要看到最新结果的请求，强制发到主库上。
2. 对于可以读到旧数据的请求，例如发布商品信息后，买家会稍微晚几秒看到最新发布的商品，走从库

* sleep

主库更新后，读从库之前 sleep（1），需要客户端的配合去解决用户的体验问题，因为一般主从的延迟都是
在1s 左右，但尽管这样，还是有可能会读取到过期的数据，毕竟不能严格控制延迟在1s内，



##### 判断主备延迟

获取主备延迟，如果延迟高则直接查主库，如果延迟低且在业务的可接受范围内，可以主动等待类似上面的 sleep
但靠这种延迟的办法并不显示，延迟0并不表示完全同步（主库客户端已经提交，但从库还没确认）

1. 通过 `show master slave`，判断 `seconds_behind_master`，精度较差
2. 直接比对 binlog position

```
master_log_file master_log_position 主库的当前位置
slave_log_file slave_log_position   从库的位置
```

3. 对比 GTID

判断主备双方的 GTID 集合是否相同

```
auto_position = 1， 确保双方使用的是 GTID 协议
retrived_gtid_set, 备库收到的日志 GTID 集合
executed_gtid_set，备库已经执行完的 GTID 集合， 
```



##### semi-sync

semi-sync 解决了事务提交后但还没传给从库的问题

1. 事务提交的时候，主库b把 binlog 发给从库
2. 从库收到 binlog 之后，发回给主库一个 ack，表示收到
3. 主库收到 ack 之后，才能给客户端返回 “事务完成” 的确认

semi + 断电判断在一主一备的场景是成立的，但在一主多从的场景中，因为 semi-sync 只要等到一个从库的 ack，
就给客户端返回确认，但如果查询的时候不是这个返回 ack 的从库，就无法确保读到最新的数据，还是会过期读。



##### 等主库位点

```
select master_pos_wait(file, pos[, timeout])

从库执行，主动等待主库 file,pos N 秒，如果失败，返回 -1
```

GTID

```
select wait_for_executed_gtid_set(gtid_set, 1)

主动等待，直到这个库执行的事务中包含传入的 gtid_set，返回0，超时为1
```



#### 数据库出问题

并发连接：具体的客户端连接数，查询通过 `show processlist`
并发查询：在事务中的实际查询数，`set global innodb_thread_concurrency = 128`



#### 误删数据

* 误删行

delete 误删数据行，通过 Flashback 工具修改 binlog 内容，拿回原库重放。确保 `binlog_format=row` 
和 `binlog_row_image=FULL`，在修改的时候，因为 row 格式记录数据行的修改事件，所以只需修改事件类型
`Delete_rows event` 修改为 `Write_rows event`

事前预防：代码 SQL 审计、`sql_safe_updates=on` 防止 delete 无条件删除

* 误删表/库

通过全量备份恢复

1. 取最近一次全量备份，如果一天一备，取0点的记录
2. 用备份的数据恢复临时库
3. 从日志备份里取0点之后的日志
4. 将除了误删除的语句全部应用到临时库上

* rm

通过 HA 系统恢复实例实例，通过 HA 系统自动上下线 MySQL 服务，保证可用


#### kill

kill 用于处理长时间等待其他事务中的锁的情况

1. 将 session 状态改为 `THD:KILL_QUERY`
2. 给 session 的执行线程发一个信号



#### 查询内存占用

取数据和发数据过程：（边读边发）

1. 获取一行，写到 net_buffer 中，内存大小由参数 `net_buffer_length` 定义
2. 重复获取行，直到 net_buffer 写满，调用网络接口发送出去
3. 如果发送成功，则清空 net_buffer，继续读取下一行，写入 net_buffer
4. 如果发送函数返回 EAGAIN 或 WSAEOULDBLOCK，就表示本地网络栈（socket send buffer）写满了，
进入等待。直到网络栈重新可写，再继续发送。

InnoDB 内部使用的是最近最少使用（Least Recently Used，LRU）算法，按照 5:3 的比例将整个 LRU 链表分成了 
young 区和 old 区。



#### Join

`select * from t1 join t2 on (t1.a = t2.a);` MySQL 优化器可能会选择 t1 或 t2 作为驱动表，可以使用 
`t1 straight_join t2 on ...` 直接连接，固定驱动表和被驱动表。

left join：左边的表不一定是驱动表，

##### Index Nested-Loop Join（NLJ）

1. 从表 t1 中读入一行数据 R
2. 从数据行 R 中，取出字段 a 到表 t2 里面查
3. 取出表 t2 中满足条件的行，和 R 组成一行，作为结果集的一部分
4. 重复过程 1-3，直到表 t1 的末尾循环结束

结论：小表驱动大表。(两个表按照各自的条件过滤，过滤完成之后，计算参与 join 的各个字段的总数据量，
数据量小的那个表，就是“小表”，应该作为驱动表)。


#### 临时表

临时表一般用于处理比较复杂的计算逻辑。由于临时表是每隔线程自己可见的，所以不需要考虑多个线程
执行同一个处理逻辑时，临时表的重命名问题。线程在退出的时候，临时表也能自动删除，省去了收尾
和异常处理的工作。

注意：因为临时表不记录 binlog，所以备库中不会有临时表

* 特点：

1. 建表 `create temporary table ...`
2. 一个临时表只能被创建它的 session 访问，对其他线程不可见。
3. 临时表的表名可以与普通表同名
4. 一个 session 内有同名的临时表和普通表的时候，`show create ...` 语句，以及增删改查访问的是临时表
5. `show tables` 不显示临时表

* 为什么可以重名

MySQL 会在 InnoDB 中创建一个 frm 文件保存表结构定义，`#sql{进程_id}_{线程_id}_系列号` 的格式



##### 临时表 vs 内存表

* 内存表：指的是使用 Memory 引擎得表，`create table ... engine=memory`。表得数据在内存中，系统重启得时候会被清空，
但表结构还在。
* 临时表：可以使用各种存储引擎类型。如果是 InnoDB 或 MyISAM 的临时表，写数据的时候是写道磁盘上的。

##### 使用

```
(select 1000 as f) union (select id from t1 order by id desc limit 2);

1. 构建一个内存临时表f
2. select1 将 1000 放入到临时表 f 中，
3. select2 查出 1000 和 999，因为冲突，只放入 999
4. 去重最后得到最终的 1000 和 999
```

```
select id%10 as m, count(*) as c from t1 group by m;

把 t1 中的数据按照 id%10 进行分组统计，并按照 m 的结果排序输出。

1. 创建内存临时表，表里有两个字段 m 和 c，主键为m
2. 扫描表 t1 的索引 a，依次取出叶子节点上的 id 值，计算 id % 10 的结果，记为x
    * 如果临时表中没有主键为 x 的行，那么插入这一行的记录 (x,1)
    * 如果表中有主键为 x 的行，就将 x 这一行的 c 值+1
3. 遍历完成之后，再根据字段 m 做排序，得到结果集返回给客户端
```



#### Memory 引擎

Memory 引擎使用的是 hash 结构的存储方式, hash(id) -> 数据行，所以当我们 `select * ` 的时候，会出现
访问的顺序并不是有序的。

##### InnoDB vs Memory

![](/img/mysql_memory_struct.png)

* InnoDB: 把数据放在主键索引上，其他索引上保存的是主键id。__索引组织表__
* Memory: 把数据单独存放，索引上保存数据位置的数据组织形式。__堆组织表__

1. InnoDB 表的数据是有序存放的，而内存表的数据是按照写入顺序存放的。
2. 当数据文件有空洞时，InnoDB 再插入的新数据的时候，为了保证数据的有序性，只能再固定的位置写入新值。
而内存表找到空位就可以插入新值。
3. 数据位置发生变化的时候，InnoDB 只需修改主键索引，而内存表需要修改所有索引
4. InnoDB 表用主键索引的时候，需要走一次索引查找，用普通索引查询的时候，需要走两次索引。
而内存表没有这个区别，所有的索引地位相同，
5. InnoDB 支持变长数据类型，而内存表不支持，因此内存表的每行数据长度相同。

##### 不建议使用

* 锁粒度问题

内存表不支持行锁，只支持表锁。如果一张表上有更新，会堵住在这个表上的读写操作。

* 持久化问题

数据存放再内存中，虽然会访问速度快，但断电时数据会丢失。再主从架构下，会造成主从的数据不一致等问题。



#### 拷贝数据

create table t1 like t;
insert... select;   会对源表加读锁，原表访问性能下降

1. mysqldump

将数据到处成为一组 INSERT 语句，然后再执行 

```
mysqldump -h$host -P$port -u$user --add-locks=0 --no-create-info --single-transaction  --set-gtid-purged=OFF db1 t --where="a>900" --result-file=/client_tmp/t.sql
```

2. 导出 csv 文件

```
select * from db1.t where a>900 into outfile '/server_tmp/t.csv';

load data infile '/server_tmp/t.csv' into table db2.t;
```

3. 物理拷贝方法

```
t -> r

create table r like t，创建一个相同表结构的空表
alter table r discard tablespace，这时候 r.ibd 文件会被删除
flush table t for export，这时候 db1 目录下会生成一个 t.cfg 文件
在 db1 目录下执行 cp t.cfg r.cfg; cp t.ibd r.ibd
unlock tables，这时候 t.cfg 文件会被删除
alter table r import tablespace，将这个 r.ibd 文件作为表 r 的新的表空间，由于这个文件的数据内容和 t.ibd 是相同的，
所以表 r 中就有了和表 t 相同的数据
```

* 恢复速度最快，但必须是全表拷贝，不能只拷贝部分数据
* 需要到服务器上拷贝数据，一般只有 DBA 才有权限完成
* 通过拷贝物理文件实现，源表和目标表得都是 InnoDB 才能使用


## 大数据分页

`select * from t order by score limt 100000, 10`;

大数据分页的主要问是查询的时候查询索引表获取所有的数据后，再根据偏移 limit offset 去筛选返回最终的数据，如果是二级索引，
那么索引后的数据还得回表之后才进行筛选返回

1. 记录下上次查询的 maxId，然后下次再根据这个 maxId，再次查询时判断 id > maxId，利用 mysql 索引查询减少返回数量
（只能用于连续访问的页面，不能跳页）

2. 利用子查询加速 

```jvm
select * from t where id >= (select id from t order by id limit 9000,1) limit 10
```

3. join

```jvm
select * from t a join (select id from t order by id limit 9000,1) b on a.id = b.id;
```


### 分库分表

单表500w，单库5000w

分库：将数据拆分成多个数据库里，主要出现在单库的并发数为大于2000。
分表：单表的数据量在达到500w-800w的时候，会出现性能瓶颈，这时候需要分表提升性能。

垂直：将部分字段拆分到其他表去，比如将较少访问频率很高的字段新建表，将较多的访问频率低的字段放到另外一张表
水平：将全部数据划分成多个表。

1.range：每个库一段连续的数据，比如按照 __时间范围__ 拆分，缺点是会很容易出现热点问题，因为大量的流量都打在了最新的数据上。
比较适合报表这类按时间区分的数据。
2.hash：比如根据 orderId 将 hash mod 分散到多个表中。


#### 分库分表的问题

1. 事务问题：

    * 采用分布式事务
    
    * 将一个跨多个数据库的分布式事务拆分成多个单个数据库上的事务，并通过应用程序控制多个小事务
    
2. 跨节点 join

    分多次查询，按照 mysql join 的方式对数据进行聚合
    
3. 跨节点 count、order by、group by 等聚合函数
    
    和 join 语义差不多，分别在各个节点上获取数据结果集之后在程序端进行合并。
    
    
4. 跨分片的排序分页

    情况比较复杂：比如我要查 limit N，M 那么我需要对多个分片的数据进行 order 后在limit，然后在程序端聚合这些数据（sort limit），
    但这样会比较消耗性能，用户越往后翻性能越差
    
    如果是前台应用，限制用户只能查看前n页（看后面的对于用户来说意义不大），如果是后台的应用，那么将 page size 的值设置大一点，
    能有效减少分页数目。（当然最好还是有大数据平台、或是es 这类进行聚合查询）