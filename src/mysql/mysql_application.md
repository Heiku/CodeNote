
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


