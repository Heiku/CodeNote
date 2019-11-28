### @CallerSensitive

在 Unsafe 中，获取单例 unsafe 对象的过程中需要进行两步关键操作：

1. 通过 Reflection.getCallerClass() 获取调用的对象类信息
2. 判断调用对象的类信息是否是属于 SystemDomainLoader，否则抛出异常

```
@CallerSensitive
public static Unsafe getUnsafe() {
    Class var0 = Reflection.getCallerClass();
    if (!VM.isSystemDomainLoader(var0.getClassLoader())) {
        throw new SecurityException("Unsafe");
    } else {
        return theUnsafe;
    }
}
```


#### CallerSensitive

@CallerSensitive 作用为找到真正发起反射请求的类。目的是为了堵住多次反射越级得到最终的对象的访问权。例如：原本我的类本来没足够的权限访问信息，
那么我可以通过双重反射达到目的，反射相关的类是有很高的权限的，而 我 -> 反射1 -> 反射2 这样的调用链上，__反射2__ 检查权限时看到的只是 __反射1__，
这样就被欺骗了，存在安全漏洞，而使用 @CallerSensitive 标记反射接口后，在 `getCallerClass()` 的时候，就能直接到达未标记的对象，即 __我__。 

找到调用的对象类之后，还需要判断对象的访问权限，通过 `VM.isSystemDomainLoader`，判断类是否时属于 BootstrapClassLoader,

tips: (Bootstrap ClassLoader, Extension ClassLoader, Application ClassLoader)

通过判断 对象类的 classLoader 不是在 Java堆中，就认定该 classLoader 是属于Bootstrap ClassLoader，UnSafe 不开放给用户，在调用时
只能通过 getUnsafe() 获取对象的实例，同时会判断是否使用 BootStrap ClassLoader 加载 Unsafe.class,(AtomicInteger 在 rt.jar 中，
属于 Bootstrap，所以可以直接使用 Unsafe.getUnsafe()，如果是用户的话，只能通过反射构造[SuperArr](/src/Basic/unsafe/UnsafeI.java))

```
public static boolean isSystemDomainLoader(ClassLoader var0) {
    return var0 == null;
}
```


### 引用

[JVM注解@CallSensitive](https://blog.csdn.net/HEL_WOR/article/details/50199797)  
[JDK8的@CallerSensitive](https://blog.csdn.net/aguda_king/article/details/72355807)