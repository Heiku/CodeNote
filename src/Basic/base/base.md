
### 值传递 引用传递

值传递：是指在调用函数的时候将实际参数 `复制一份` 传递到函数中，这样在函数中如果对参数进行修改，将不会影响到实际参数。  
引用传递：是指在调用函数时将实际参数的地址 `直接 ` 传递到函数中，那么在函数中参数所进行的修改，将影响到实际参数。

关键：区别在于实参到底有没有复制一份给形参, Java（值传递）传递引用的时候，实际上时传递的是引用的拷贝，都指向同一片对象堆
内存区域   
[PassByValueOrRef.java](/src/Basic/base/PassByValueOrRef.java)


### hashCode()

对象的 hashCode 作为对象的唯一标识符

Object 对象将返回对象的内存地址  

```
public native int hashCode();
```

而 String 中采用的是利用素数计算散列的方法，来计算 String.hashCode() 


```
public int hashCode() {
    int h = hash;
    if (h == 0 && value.length > 0) {
        char val[] = value;

        for (int i = 0; i < value.length; i++) {
            h = 31 * h + val[i];
        }
        hash = h;
    }
    return h;
}
```

这里，为什么会把 __31__ 作为散列的计算值呢？  

* 把 31 作为乘数，主要在于性能的考虑。因为 31 是一个奇素数，如果乘数采用偶数，当乘法溢出低位移位补0，导致部分 hash 信息丢失。
同时，31 有个很好的性能，JVM 中进行了优化，即用移位和减法代替了乘法，可以得到更好的性能 `31 * i == (i << 5） - i`，
31 在性能和溢出上做了平衡，如果使用 63，那么就有可能会更容易溢出。


### 异常

* Checked Exception: 编译器可检测的异常（ClassNotFoundException、IOException）
* Runtime Exception: 运行期间产生的异常（NullPointerException、IndexOutOfBoundsException）

### ClassLoader

```
class A{
    public void m(){
        A.class.getClassLoader().loadClass("B");
    }
}
```

一般在类加载的过程有三种概念上的 ClassLoader 提供使用：

* CurrentClassLoader: 当前类加载器（CCL），在代码中对应就是类型 A 的类加载器
* SpecificClassLoader: 指定类加载器（SCL），代码中对应的是 `A.class.getClassLoader()`，如果使用任意的 ClassLoader 进行加载，
这个 ClassLoader 可以称之为 SCL
* ThreadContextClassLoader: 线程上下文类加载器（TCCL），每个线程都会拥有一个 ClassLoader 引用，而且可以通过 
`Thread.currentThread().setContextClassLoader(ClassLoader.classLoader) 进行切换`


### 引用
[](https://www.zhihu.com/question/31203609)