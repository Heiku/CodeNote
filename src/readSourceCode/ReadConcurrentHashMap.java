package readSourceCode;

public class ReadConcurrentHashMap {

    static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * 0100 0000   >>>1  0010 0000  |=  0110 0000
     * 0110 0000   >>>2  0001 1000  |=  0111 1000
     *  ...
     *
     * 使最高位上的1后面的位全为1，最后 +1 得到我们想要的 2的整数次幂的值
     *
     * 例如：10, 0000 1010  -> 0000 1111 (15) + 1 = 16
     *
     * @param cap
     * @return
     */
    static final int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }


    /**
     * 首先：为什么hashMap的长度要保持在 2的整数幂？
     *      因为这样, hash() & len - 1 的时候，才能构造出 0000 1111 类似这样的低位掩码
     *      类似于 0010 0101
     *          & 0000 1111
     *                 0101
     *      这样就将散列值的高位清零，只保留地位的值，用于访问数组下标
     *
     *
     * 其次，为什么hash() 能减少发生冲突的概率？
     *      因为hashMap 在1.8里加入了 扰动函数 -> hash()， h = hashcode(key) ^ (h >>> 16)
     *      意思就是说将hashcode的 左16 与 右16进行异或，充分混乱hash码的高位与低位，以此增加了低位的随机性
     *
     * @param key
     * @return
     */
    static final int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }


    /**
     * 为什么HashMap不安全？  这个问题可以从两方面入手，get,put操作和resize扩容。
     *
     * 首先，多线程在插入数据时，进行resize扩容的过程中，每个线程都有机会进入到方法中进行扩容操作，而在1.7中，因为
     *      扩容后的链表会倒叙，所以但线程切换时，有可能会出现上一个线程持有数组下标节点，而下一个线程已经修改扩容完了，
     *      所以上一个线程在持有节点的时候，不知道已经扩容完，等到其扩容时，节点间形成环，导致cpu 100%
     *
     *      这个问题在1.8中已经解决，通过head，tail两个节点保留头尾节点，是链表经过扩容后，方向不变
     *
     * 其次，最主要的还是日常操作中的put(),get(),size(),contain()等方法中，没有使用锁，导致多线程中数据替换丢失、
     *      数据访问不一致的问题
     *
     *
     * @param args
     */


    /**
     * ConcurrentHashMap为什么能保证安全：
     *      1.7 分段锁，segment extends ReentrantLock
     *      1.8 采用CAS + Synchronized，
     *
     *   put(): 首先K V都不允许为空，否则报空指针异常， 获取hash
     *          接着，开始遍历数组桶，如果table为空，则采用cas  initTable的方式初始化
     *          然后，通过hash值去定位数组桶，casTabAt()，得到头节点，
     *             注意这里 hash = -1 ：正在扩容，ForwardingNode节点扩容
     *      *                     hash = -2 ：数节点
     *          synchronized的方式锁住头节点，然后链表替换、添加，或者树节点的添加替换
     *
     *          最后还得判断长度是否大于8，转成树，还得记录对应的修改次数，容量加一
     *
     *  get(): 为什么get()不需要加锁，就可以得到最新的值，而不会脏读？
     *          因为节点数组是通过 volatile修饰的，
     *
     *          这里得注意，volatile修饰数组的时候，并不会对数组中的每一个元素具有可见性，而是保证整个数组地址的可见。
     *
     *          数组采用volatile修饰，加上Node节点中的 val,next 也是volatile修饰，这就保证了读取时总是最新的，所以不需要加锁
     *
     * size()： 添加 addCount()：添加的两个对象分别是 baseCount + CounterCell[]，都使用volatile进行修饰
     *              一般来说采用 cas的方式增长 baseCount，如果失败的话，那么就采用 cas的方式添加到 CounterCell[]中，
     *                      如果还是失败，采用for(;;)死循环，添加，
     *              在添加的过程可以 通过参数 check去检查扩容
     *
     *          统计长度的话，就可以采用 baseCount + for()遍历 ceilCounter[]的方式  累加
     *
     *
     *
     * 扩容分析： ConcurrentHashMap在扩容上与1.7的时候差别很大，主要是引入了多线程扩容的概念，而不是锁住单独扩容
     *
     *    helpTransfer(): 这里引入一个 SizeCtl的变量概念，-1：table正在初始化，-N：表示N-1个线程正在扩容， >0：初始化完成,
     *                   同时高低位分别保存着不同的概念： sizeCtl(高16：length标识符， 低16：并发扩容的线程数)
     *
     *              主要过程是循环判断对sizeCtl的判断或者修改，通过sizeCtl 判断(标识符修改？扩容结束？达到最大扩容线程数？)
     *                否则，通过cas sizeCtl + 1，进行transfer
     *
     *    transfer()： 首先，先确定为每个CPU分配对应的桶大小，默认16，保证每个CPU能处理一样多的桶
     *                 接着，创建一个两倍长度的 Node[]，初始化ForwardingNode(newTab)， 处理完槽点位的节点后，表示该槽位处理过了
     *                 然后，for(int i = 0，bound = 0;;) 去遍历槽位确定对应的链表头。
     *                      while（advance） 为每个线程去确定它所需要处理的槽区间，当处理完后，可以去剩余空间领取槽位空间（A | B | C | 最先处理完的线程领取这个剩余区间）
     *
     *                在处理前，判断tab == null ？ cas修改为fwd节点，如果hash == -1，表示其它线程在修改了，advance = true，跳过
     *
     *                synchronized(f),对头节点进行加锁，创建高低节点 lowNode、highNode， runBit = fh & n  0 | 1，接着遍历，每个node的hash & n == runBit，区分高低节点
     *                最后再次遍历链表，将链表中的节点按照位数分成两条链表，获得的两条链表方向相反，casTabAt 放入数组的高低位中，设置forwardingNode，advance = true
     *
     *                 如果槽位节点是红黑树节点，还是根据高低位lo，hi，树节点转换成两条链表，接着再将链表重新构造红黑树
     *
     *
     * @param args
     */

    public static void main(String[] args) {
        System.out.println(tableSizeFor(9));



    }
}
