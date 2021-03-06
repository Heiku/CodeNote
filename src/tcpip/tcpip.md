

### TCP 三次握手

TCP 连接使用三次握手的原因：避免历史错误连接的建立，减少通信双方不必要的资源消耗

#### 如果建立连接时的通信只有两次？  

一旦发送方在发出建立连接的请求之后，就没办法撤回这个请求。当在网络状况复杂或者比较差的环境中，发送方没有收到应答，连续
发送请求连接请求，接收方只能接收或拒绝请求，导致发送方无法知道当前的连接状态（可能在建立后，接收方返回多个拒绝请求）。
所以在三次连接中引入了 RST 消息标志，接收方当收到请求时会将发送方发来的 `SEQ + 1` 发送回接收方，由接收方判断当前连接是否
为历史连接：

* 如果当前连接是历史连接，即 `SEQ` 过期或者超时，那么发送方就会直接发送 `RST` 控制消息中止这一次连接
* 如果当前连接不是历史连接，那么发送方就会发送 `ACK` 控制消息，通信双方就会成功建立连接

使用三次握手和 RST 控制消息是将是否建立连接的控制权交由发送方，因为只有发送方有足够的上下文判断当前连接是否是错误的或者是
过期的。

#### TIME_WAIT

* 防止延迟的数据段被其他使用相同源地址、源端口、目的地址以及目的端口的 TCP 连接收到
* 保证 TCP 连接的远程被正确关闭，即等待被动关闭连接的一方收到 FIN 对应的 ACK 消息

如果客户端等待的时间不够长，当服务端还没有收到 `ACK` 的时候，客户端就重新与服务端建立 TCP 连接就会造成以下问题：  
服务端因为没有收到 `ACK` 消息，所以认为当前连接还是合法的，客户端重新发送 `SYN` 消息请求握手时，会收到服务端的 `RST`
 消息，连接建立的过程就会被终止。

#### 同时维护着 初始序列号

* 数据包被发送方多次发送造成数据重复 （重复去除）
* 数据包在传输的过程中被路由或者其它节点丢失（重新发送）
* 数据包在接收方可能无法按照发送顺序（重排序）

### DNS (Domain Name System)

1. 先由本地的 DNS Client 向 DNS Resolver 解析器发出解析 `heiku.github.io` 的请求
2. DNS Resolver 首先向就近的根 DNS 服务器 `.` 请求顶级域名 DNS 服务的地址
3. 拿到顶级域名 DNS 服务 `io.` 的地址之后会向顶级域名服务请求负责 `github.io.` 域名解析的命名服务
4. 得到授权的 DNS 命名服务就可以根据请求的具体的主机记录直接向该服务请求域名对应的 IP 地址

（.）根域名、(io.)顶级域名、（github.io）二级域名、（heiku.github.com）子域名

#### 实现 （TCP & UDP）

DNS 查询的类型不止包含 A 记录（ip指向）、CNAME 记录（别名）等常见查询，还包括 AXFR 类型的特殊查询，这种特殊查询主要用于
 _DNS 区域传输_， 它的作用就是在多个命名服务器之间快速迁移记录，由于查询返回的响应比较大，所以会使用 TCP 协议进行传输
数据包。
 
在 _域名查找_ 的过程中，因为数据量小，且不需要稳定的连接，同时 TCP 在建立连接（三次握手），销毁连接（四次握手）会带来很大
的额外开销，这种情况在 DNS 解析器递归地与多个命名服务器进行通信时加倍开销。

但是，在区域查询中，因为一个 UDP 数据包的大小最多可以达到 64KB，虽然对于 DNS 查询已经时一个很大的数值（通常20），
但在实际生产中，一旦数据包中的数据超过传送链路中的最大单元（MTU，单个数据包大小的上限，一般为 1500 字节），当前数据包
就可能被分片传输、丢弃、部分的网络设备甚至会拒绝处理包请求，导致 UDP 协议在 __区域传输__ 中的不稳定，而 TCP 通过
序列号、重传等机制保证了传输过程中的可靠性，所以使用 TCP。


### 拥塞控制 & 流量控制

* _拥塞控制_ ：防止过多的数据包注入到网络中，防止了网络中的路由器或链路不至于过载。

* _流量控制_ ：往往指的是点对点通信的控制，抑制发送端发送的速率，以便接收端来得及接收。 

##### 为什么要进行拥塞控制

网络中的路由器会有一个数据包处理队列，当网络中存在太多的数据包时，处理队列一下子就被占满然后抛弃新来的数据包。而上层 TCP
 协议会认为数据包在网络中丢失，重新发送，但路由器又会继续丢弃，这样导致了网络性能的急剧下降，引起网络瘫痪。

#### 实现

慢开始、拥塞避免、快重传、快恢复

* _慢开始_ : 发送方按照发送窗口大小 cwnd 发送数据，接收方成功接收数据包后回复确认，这样发送方会继续调整 cwnd 的大小，
cwnd = 1, 2, 4 ..，慢开始的 "慢" 指的是并非一开始就将大量的数据包发给接收端，而是通过这种指数的方式预先增长发送窗口的大小。

* _拥塞避免_ : 由于指数增长会很快膨胀 cwnd 值，所以需要一个阈值 ssthresh 限制发送窗口大小，当 cwnd > ssthresh 时，开始使用
拥塞避免算法，从原来得 指数增长 -> 线性增长，cwnd + 1 

* _快重传_ : 原本当慢开始和拥塞避免在发送数据之后，会开启一个计时器，如果在指定时间内接收方没有发来确认时，执行乘法减小，
而 _快重传_ 让发送方在连续收到 3个重复的确认后就可以开始执行 _乘法减小_ 而不需要等待所设置的重传计时器到时。这就需要了
接收方在收到一个 _失序的报文段_ 时立即发送重复的确认。

* _快恢复_ : 当网络出现拥塞时并乘法减小（ssthresh * 1/2），并不是设置 cwnd = 1 并重新开始慢开始，而是让 cwnd = 乘法减小
后的新 ssthresh，并开始执行 _拥塞避免_ (cwnd + 1)


##### TCP 的性能问题

1. TCP 的三次握手带来了额外的开销，这些开销不只包括需要更多的数据，还是增加了首次传输数据的网络延迟。
2. TCP 的拥塞控制在发生丢包的时候会进行退让，减少能发送的数据段数量（快重传），但是丢包不一定说明发生了网络拥塞，更多的可能是
网络的状态差。

### ssl/tsl

SSL(Secure Sockets Layers) / TLS (Transport Layer Security)

1. 信息通过加密传输，第三方无法窃取
2. 具备校验机制，一旦被篡改，通信双方会立刻被发现
3. 配备身份证书，防止身份被冒充

#### 握手过程

![](/img/ssl-tsl.png)

##### Client Hello

1. 支持的协议，如 TLS 1.0
2. 支持的加密算法，如 RSA 公钥加密
3. 支持的压缩算法
4. _随机生成数_，用于生成 “对话密钥”

##### Server Hello

1. 确认使用的加密通信协议，如果支持版本不一致，则关闭
2. 确认使用的加密方法，如 RSA
3. 服务端证书
4. _随机生成数_，用于生成 “对话密钥”

响应之后，客户端将会持有 server random, 服务端将持有 client random  
（如果是安全系数较高的业务，通常会要求客户端提供安全证书）

##### Client Response

客户端在收到 Server Hello 之后，首先会验证 _服务器证书_ ，如果证书不是可信机构颁布、过期或者证书与域名不一致等，就会像
访问者显示一个警告，由访问者决定是否要继续通信。

如果没问题，将从证书中获取服务器的 _公钥_，
1. _生成随机数 (pre-master key)_，该随机数通过 _公钥_ 加密，防止被窃听
2. 编码通知改变，表示随后的信息都将通过双方商定的加密方法和密钥进行发送

##### Server Response

服务端收到客户端发来的第三个 random number（encrypted by public key from certificate）,这样双方都拥有了 三个随机数，然后
双方就会按照这三个随机数进行对称加密通信

1. 编码通知改变，表示随后的信息将用双方协定的加密方法和密钥发送
2. 服务器握手结束通知，表示服务器的握手阶段已经结束，这一项同时也是前面发送内容的 hash 值，用来供客户端校验


### "粘包" 问题

TCP 协议是面向字节流的传输层通信协议（没有消息边界），尽管发送方发送10,20,50这几个长度不同的数据包，但对于接收方而言，并不可知，而像是整体一样，
按自己的缓冲区大小有多少读多少。

#### Nagle 算法

Nagle 算法是一种通过减少数据包的方式提高 TCP 传输性能的算法。由于带宽有限，它不会将小的数据块直接发送到目标主机中，
而是会在本地缓冲区中等待更多的待发送的数据 （它会等待缓冲区中数据超过最大数据段（MSS）或者上一个数据段被 ACK 时，
才会发送缓冲区中的数据），这种批量发送数据的策略虽然会影响实时性和网络延迟，但能降低网络拥堵的可能性并减少额外开销。
（现在因为网络带宽不像以前那么紧张，所以 Linux 默认关闭 Nagle 算法：TCP_NODELAY = 1）

Nagle 算法虽然能够在数据包较小时提高网络带框的利用率，并减少 TCP 和 IP 协议头带来的额外开销，但是使用 Nagle 算法后可能
会导致应用层协议多次写入的数据被拆分发送，当接收方从 TCP 协议栈中读取数据时，会发现不相关的数据出现在同一个数据段中，
应用层协议可能没有办法对它们进行拆分和重组。

`TCP_CORK` 也是类似的延迟数据的发送，通过延迟发送数据来提高带宽的利用率，它们会对 __应用层__ 的数据进行拆分和重组， 
这类机制和配置能出现的原因是：TCP 是基于字节流的协议，其本身没有数据包的概念，
不会按照数据包发送数据。


#### 消息边界

既然 TCP 是基于字节流的，这意味着应用层协议要自己划分消息的边界，最常用的两种是基于 __长度__ 和基于 __终结符（Delimiter）__。

基于长度的实现有两种方式，一种是使用固定的长度，所有的应用层都使用统一的大小，另一种方式是使用不固定长度，但是需要在
应用层协议的协议头中增加负载长度的字段。这样接收方才能从字节流中分离出不同的消息，HTTP 协议的消息边界就是基于长度实现。

在 HTTP 中，使用了 `Content-Length` 头表示 HTTP 消息的负载大小，当应用层解析到足够的字节数后，就能从中分离出完整的 HTTP
 消息。不过 HTTP 协议除了使用基于 __长度__ 的方式实现边界，也会使用基于终结符的策略，当 HTTP 使用块传输 (Chunked Transfer)
 机制时，HTTP 头将不再包含 `Content-Length`，而是使用负载大小为 0 的 HTTP 消息作为终结符表示消息边界。
 （也可以直接传 JSON 数据，以 JSON 数据是否能正确被解析判断消息是否终结）


#### TCP/IP

当应用层协议使用 TCP/IP 协议传输数据时，TCP/IP 协议簇可能会将应用层发送的数据分成多个包依次发送，而数据的接受方收到的
数据可能是分段的或者是拼接的，所以它需要将对应的接收的数据进行重组。

* IP 协议会分片传输过大的数据包（Packet）避免物理设备的限制
* TCP 协议会分段传输过大的数据段（Segment）保证传输的性能

##### MTU

IP 协议是用于传输数据包的协议，作为网络层的协议，它提供了数据的路由和寻址功能，让数据通过网络到达目的地。
不同设备之间传输数据，需要先确定一个 IP 数据包的大小上限，即最大传输单元（Maximum transmission unit）MTU，
MTU 是 IP 数据包能够传输的上限。（超过 MTU 会导致丢包）

MTU 的值不是越大越好，更大的 MTU 意味着更低的额外开销，更小的 MTU 意味着更低的网络延迟。每一个设备上都有自己的 MTU，
两个主机之间的的 MTU 依赖于底层的网络能力，它由整个链路上 MTU 最小的物理设备决定。

```
1000 - 2000 - 1200 

MTU: 1000

一般情况下，IP 主机的l路径 MTU 都是1500，去掉 IP首部的 20字节，如果传输的数据大于 1480，那么将会进行数据包分片

以 UDP/IP 为例: 
假设传输 2000 字节的数据，加上 UDP 8 字节的协议头，IP 协议需要传输 2008 字节的数据。

1. 20（IP 协议头） + 8（UDP 协议头）+1472（字节数据）
2. 20（IP 协议头） + 528（字节数据）

note：第二个数据包中不包含 UDP 协议的相关信息，一旦发生丢包，整个 UDP 数据报将无法重新拼接。
如果 UDP 数据报传输的数据过多，那么 IP 协议就会大量分片，增加了不稳定性。  
``` 

#### MSS

TCP 协议是面向字节流的协议，应用层交给 TCP 协议的数据并不会以消息为单位向目的主机发送，应用层交给 TCP 协议发送的
数据可能会拆分成多个数据段。

TCP 协议引入了最大分段大小（Maximum Segment Size）MSS，它是 TCP 数据所能携带的数据上限。默认 MSS = MTU - 40 = 1460

IP 协议的 MTU 是物理设备上的限制，它限制了路径上能够发送数据包的上限，而 TCP 协议的 MSS 是操作系统内核层面的限制，
通信双方会在第三次握手时确定这次连接的 MSS。一旦确定了 MSS，TCP 协议就会对应用层交由 TCP 协议发送的数据进行拆分，
构成多个数据段。

```
如果 TCP 连接的 MSS 为1460字节，应用层想通过 TCP 协议传输2000字节，那么 TCP 将按照 MSS 将2000字节进行拆分：

1. 20（IP 协议头）+ 20（TCP 协议头）+ 1460
2. 20（IP 协议头）+ 20（TCP 协议头） + 540
```

TCP 为了保障可靠性，会通过 IP 协议的 MTU 计算出 MSS 并根据 MSS 分段避免 IP协议对数据包的分片。如果不加限制，
IP 协议的分片会导致部分数据包失去传输层协议头，一旦数据包丢失就只能丢旗全部数据。

TCP 协议拆分数据是为了保证传输的可靠性和顺序，作为可靠的传输协议，为了保证数据的传输顺序，它需要为每一个数据段
增加包括序列号的 TCP 协议头，如果数据段大小超过 IP 协议的 MTU 限制，那么在分片分段的过程中会带来额外的重传
和重组开销，影响性能。


### 总结

1. TCP 协议是基于字节流的传输层协议，其中不存在消息和数据包的概念
2. 应用层协议没有使用长度或者基于终结符的消息边界，导致多个消息粘连


### 引用

[为什么 DNS 使用 UDP 协议 · Why's THE Design?](https://draveness.me/whys-the-design-dns-udp-tcp)  
[SSL/TLS协议运行机制的概述](https://www.ruanyifeng.com/blog/2014/02/ssl_tls.html)  
[如何理解传输层的TCP面向字节流，UDP面向报文？](https://www.zhihu.com/question/341865775)  
[TCP 流协议和消息分帧的理解](https://blog.csdn.net/lianliange85/article/details/50194975)  
[为什么 TCP 协议有粘包问题](https://draveness.me/whys-the-design-tcp-message-frame)  