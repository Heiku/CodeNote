
[教你用Java字节码做点有趣的事](https://juejin.im/post/5b51ff276fb9a04f914a922e)

### 字节码

字节码（Byte-Code）是一种包含执行程序，由一序列 op 代码/数据对组成的二进制文件。通常每个操作码是一字节长，所以字节码的程度
是根据一字节来的。

实现方式：通过编译器和虚拟机，编译器将源码编译成字节码，特定平台上的虚拟机器将字节码转译成可直接执行的指令，在 Java 中，
通过 `javac` 将源文件（.java）变成字节码 (.class）。


### 栈帧

栈帧：局部变量表 + 操作数栈 + 动态链接 + 方法的返回地址  

方法调用在 JVM 中转换成字节码执行，字节码执行的数据结构就是栈帧 （stack frame），也就是在虚拟机栈 （JVM Stacks）中的栈元素。
虚拟机会为每个方法分配一个栈帧，根据栈的特性（LIFO 先进后出），栈顶的元素就是当前线程正在活动的栈帧（Current Stack），
字节码的执行操作(invoke)，也就对应着对当前栈帧数据结构的操作。


#### 局部变量表

局部变量表是一组变量值存储空间，用于存放方法参数和方法内部定义的局部变量。  

局部变量表的容量是以 slot 为最小单位，32位的虚拟机中一个 slot 可以存放 32位以内的数据类型（boolean, byte, char, short, int,
float, reference, returnAddress）

[LocalVariableTableDemo](/src/Basic/asm/LocalVariableTableDemo.java)


#### 操作数栈

操作数栈：JVM 解释执行引擎（基于栈的执行引擎）

操作数栈和局部变量表一样，在编译期间决定了存储空间，通过 Code 属性存储在类或接口的字节流中，（LIFO 栈）

#### 动态链接

动态链接就是将符号引用的方法，转换成方法的直接引用。

符号引用就是字符串，这个字符串包含足够的信息，以供实际使用时可以找到相应的位置。你比如说某个方法的符号引用，
如：“java/io/PrintStream.println:(Ljava/lang/String;)V”。里面有类的信息，方法名，方法参数等信息。  
  
当第一次运行时，要根据字符串的内容，到该类的方法表中搜索这个方法。运行一次之后，符号引用会被替换为直接引用，下次就不用搜索了。
直接引用就是偏移量，通过偏移量虚拟机可以直接在该类的内存区域中找到方法字节码的起始位置。

[JVM里的符号引用如何存储？](https://www.zhihu.com/question/30300585)