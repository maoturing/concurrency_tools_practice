<!-- TOC -->

- [3. ThreadLocal](#3-threadlocal)
    - [3.1 ThreadLocal 常用方法](#31-threadlocal-常用方法)
    - [3.2 ThreadLocal 原理与源码分析](#32-threadlocal-原理与源码分析)
        - [3.2.1 Thread、ThreadLocalMap 和 ThreadLocal](#321-threadthreadlocalmap-和-threadlocal)
        - [3.2.2 initialValue 设置初始值](#322-initialvalue-设置初始值)
        - [3.2.3 set 设置ThreadLocal值](#323-set-设置threadlocal值)
    - [3.3 ThreadLocalMap 处理哈希冲突](#33-threadlocalmap-处理哈希冲突)
    - [3.4 内存泄漏](#34-内存泄漏)
        - [3.4.1 为什么使用弱引用](#341-为什么使用弱引用)
        - [3.4.2 ThreadLocal 最佳实践](#342-threadlocal-最佳实践)
- [待补充](#待补充)
- [参考文档与推荐阅读](#参考文档与推荐阅读)

<!-- /TOC -->

# 3. ThreadLocal

对一个银行账户进行存款和取款操作，需要对共享资源账户进行加锁，同时存款和取款就会出现线程安全问题。加锁是一种处理线程安全的典型策略，针对共享资源加锁，避免线程安全问题。

在现实中，每个人都拥有自己的账户，假如银行让两个人共用一个账户，就会发生A存了100元，B存了50元，最后二者发现自己的存款都是150元，这样显然是不合适的。这种策略就是对于某个资源，每个线程都拥有该资源的副本，线程不共享。应用这种思想到并发编程中就是 ThreadLocal。


**使用场景1：** **每个线程需要一个独享的对象**，通常是工具类，典型需要使用的类有SimpleDateFromat和Random。

**使用场景2：** **每个线程内需要保存全局变量（属性）**，例如在拦截器中获取用户信息，保存到某个属性中，可以让不同方法直接使用，避免参数传递的麻烦


> 需求：将秒转换为本地日期时间

1. 2个时间转换任务，启动2个线程使用自己的局部变量 SimpleDataFormat 对象。局部变量线程私有，线程安全。[代码示例见Github](https://github.com/maoturing/concurrency_tools_practice/tree/master/src/main/java/threadlocal/ThreadLocalNormalUsage00.java)

2. 1000个时间转换任务，如果启动 1000 个线程，线程过多创建开销大，使用固定线程池10个线程来处理任务，但是每个线程都使用自己的局部变量 SimpleDataFormat 对象用于转换时间，就需要创建 1000 个 SimpleDataFormat 对象。[代码示例见Github](https://github.com/maoturing/concurrency_tools_practice/tree/master/src/main/java/threadlocal/ThreadLocalNormalUsage02.java)
   
3. 为了解决版本2中1000个任务需要创建 1000 个 SimpleDataFormat 对象，对象频繁创建与销毁，导致内存和GC开销大，使用线程池，SimpleDataFormat 对象保存到全局变量（属性）中，这样 1000 个只需要创建 1 个SimpleDataFormat 对象，节省了内存和 GC 开销。但是出现了线程安全问题。全局变量线程共享，线程切换导致线程不安全。[代码示例见Github](https://github.com/maoturing/concurrency_tools_practice/tree/master/src/main/java/threadlocal/ThreadLocalNormalUsage03.java)

4. 为了解决版本3中 SimpleDataFormat 对象转换时间过程不安全，我们加锁来保证线程安全，缺点是1000个任务串行执行，耗时较长。[代码示例见Github](https://github.com/maoturing/concurrency_tools_practice/tree/master/src/main/java/threadlocal/ThreadLocalNormalUsage04.java)
   
5. 最佳实践：ThreadLocal，每个线程都有一份 SimpleDataFormat 对象副本，线程不共享，则线程安全，每个线程一个对象共 10 个对象，节省了内存，并行执行耗时较短。综上，**ThreadLocal兼顾了线程安全、耗时较短和节省内存**。[代码示例见Github](https://github.com/maoturing/concurrency_tools_practice/tree/master/src/main/java/threadlocal/ThreadLocalNormalUsage05.java)


> **面试题：** 既然 ThreadLocal 是每个线程一份SimpleDateFormat对象，那和使用局部变量每次创建新对象有什么区别？

局部变量中 1000 个时间转换任务需要创建和销毁 1000个 SimpleDateFormat 对象，而 ThreadLocal 是每个线程 1份 SimpleDateFormat 对象，线程池共 10 个线程，所以共 10 个 SimpleDateFormat 对象，节省了内存，避免了对象频繁创建于销毁。

**ThreadLocal 的两个作用：**
1. 对象线程隔离，每个线程都有自己的对象副本
2. 任何方法都可以轻松获取到对象


**ThreadLocal的优点**：
1. 线程安全，每个线程拥有一个对象副本
2. 不需要加锁，执行效率高
3. 节省内存开销，每个线程拥有一个对象副本，避免了每个任务创建一个新对象


## 3.1 ThreadLocal 常用方法
- `set() get()` 为ThreadLocal设置当前线程对应的值
```java
    @Test
    public void test() throws InterruptedException {
        ThreadLocal<String> local = new ThreadLocal<>();
        System.out.println(local.get());
        local.set("mwq");
        local.set("123");

        System.out.println(local.get());
    }
```
- `initialValue()` 重写ThreadLocal#initialValue() 方法设置初始值，**延迟加载**，initialValue()在第一次调用get时执行。也可以使用lambda方式 ThreadLocal.withInitial()
```java
    // 重写initialValue方法
    @Test
    public void testInit() {
        ThreadLocal<String> local = new ThreadLocal<String>() {
            @Override
            protected String initialValue() {
                return Thread.currentThread().getName();
            }
        };
        System.out.println(local.get());
    }
    // lambda方式，与上面方法等价
    @Test
    public void testInit2() {
        ThreadLocal<String> local = ThreadLocal.withInitial(() -> Thread.currentThread().getName());
        System.out.println(local.get());
    }
```
- `remove()` 删除当前线程中 key 为 ThreadLocal 的Entry

```java
    @Test
    public void testRemove() throws InterruptedException {
        ThreadLocal<String> local = new ThreadLocal<>();
        System.out.println(local.get());
        local.set("mwq");
        local.remove();

        System.out.println(local.get());
    }
```



## 3.2 ThreadLocal 原理与源码分析

### 3.2.1 Thread、ThreadLocalMap 和 ThreadLocal

查看 Thread 类的源码，可知每个 Thread 对象都有一个 map 字段来保存所有 ThreadLocal 变量对应当前线程的值。
```java
public class Thread implements Runnable {
    //....
    // 当前线程相关的所有ThreadLocal值，保存在map中
    ThreadLocal.ThreadLocalMap threadLocals = null;

    ThreadLocal.ThreadLocalMap inheritableThreadLocals = null;
```


使用 ThreadLocal 的示例代码如下所示：
```java
/**
 * 每个Thread 对象都有一个ThreadLocalMap来保存所有ThreadLocal变量
 *
 * 下面代码线程中ThreadLocalMap保存了两个ThreadLocal变量
 * 线程0中两个变量：[k=s1,v="Thread-0-s1"]  [k=s2,v="Thread-0-s2"]
 * 线程1中两个变量：[k=s1,v="Thread-1-s1"]  [k=s2,v="Thread-1-s2]
 * 说明："Thread-1-s2"，表示线程1中对象s2的副本
 *
 * 使用ThreadLocal不会出现线程1输出o1.get()得到Thread-1-o2的线程不安全问题
 */
public class ThreadLocalMapDemo {
    public static ThreadLocal<String> s1 = new ThreadLocal<>();
    public static ThreadLocal<String> s2 = new ThreadLocal<>();

    public static void main(String[] args) {
        // 线程0对两个对象设置值，并输出值
        new Thread(() -> {
            s1.set(Thread.currentThread().getName() + "-s1");
            System.out.println(s1.get());

            s2.set(Thread.currentThread().getName() + "-s2");
            System.out.println(s2.get());
        }).start();

        // 线程1对两个对象设置值，并输出值
        new Thread(() -> {
            s1.set(Thread.currentThread().getName() + "-s1");
            System.out.println(s1.get());

            s2.set(Thread.currentThread().getName() + "-s2");
            System.out.println(s2.get());
        }).start();
    }
}
```
![Thread、ThreadLocalMap、ThreadLocal三者关系图](https://raw.githubusercontent.com/maoturing/PictureBed/master/pic/20200317171839.png)

Thread、ThreadLocalMap、ThreadLocal三者关系如上图所示，
1. 一个 Thread t 有且仅有一个 ThreadLocalMap 对象，后者是前者的属性；
2. ThreadLocalMap 与 ThreadLocal 的关系是`1:n`，因为一个 ThreadLocalMap 可以保存 n 个 ThreadLocal 键值对；
3. 1个ThreadLocal 对象可以被多个线程共享，ThreadLocal key 与 对象 value 的关系是`1:m`，1 个 ThreadLocal key 在 m 个线程中都有一份 value 副本。
4. ThreadLocal 对象不持有不保存 Value，Value 保存在当前线程的 ThreadLocalMap 中，其中 key 为 ThreadLocal。

**每个线程 Thread 中都持有一个属性 ThreadLocalMap 来保存所有 ThreadLocal 值**，ThreadLocalMap 的 key 为 ThreadLocal 变量，value 为该 ThreadLocal 变量对应当前线程的对象副本。其中 ThreadLocalMap 源码如下所示：
```java
static class ThreadLocalMap {
    // key 为ThreadLocal的弱引用，value为对象强引用
    static class Entry extends WeakReference<ThreadLocal<?>> {
        /** The value associated with this ThreadLocal. */
        Object value;

        Entry(ThreadLocal<?> k, Object v) {
            super(k);
            value = v;
        }
    }
```

### 3.2.2 initialValue 设置初始值
`initialValue()`：对象初始化在 ThreadLocal 第一次调用 get() 方法，延迟加载。比如 SimpleDateFormat 对象的格式是固定的，就可以使用 `initialValue()`。查看ThreadLocal源码可知，设置初始值一共有两种方法：
1. 重写 initialValue() 方法
```java
public static ThreadLocal<String> str = new ThreadLocal<String>(){
    @Override
    protected String initialValue() {
        return "test";
    }
};
```
2. 使用 lambda 表达式

```java
 public static ThreadLocal<String> str = ThreadLocal.withInitial(
     ()-> "test");
```

查看 ThreadLocal 源码可知，使用 initialValue() 设置初始值，在第一次调用 ThreadLocal#get 方法时才会调用 initialValue() 方法设置初始值并保存到 ThreadLocalMap 中。

```java
public class ThreadLocal<T> {
    // 默认实现返回null，重写后调用重写的initialValue方法
    protected T initialValue() {
        return null;
    }

    // 与上面的作用类似，使用lambda表达式
    public static <S> ThreadLocal<S> withInitial(Supplier<? extends S> supplier) {
        return new SuppliedThreadLocal<>(supplier);
    }

    // get时调用initialValue()，对对象初始化
    public T get() {
        Thread t = Thread.currentThread();
        // 得到当前线程t的ThreadLocalMap属性
        ThreadLocalMap map = getMap(t);

        if (map != null) {
            // map不为空，则使用当前ThreadLocal对象this作为key获得map中对应的value
            ThreadLocalMap.Entry e = map.getEntry(this);
            if (e != null) {
                // 返回当前ThreadLocal对象this对应的对象副本value
                T result = (T)e.value;
                return result;
            }
        }

        // 如果map为空或map中不存在当前当前ThreadLocal对象this的key，
        // 则调用initialValue()方法初始化
        return setInitialValue();
    }

    private T setInitialValue() {
        // 调用重写的initialValue()方法初始化该ThreadLocal
        T value = initialValue();
        Thread t = Thread.currentThread();
        // 返回当前线程t的属性ThreadLocalMap
        ThreadLocalMap map = getMap(t);

        if (map != null)
            // 1. map不为空，将[this，value]保存到map中
            map.set(this, value);
        else
            // map为空，为当前线程t创建ThreadLocalMap，将[this，value]保存到map中
            createMap(t, value);
        return value;
    }

    // 为线程t创建ThreadLocalMap，将[this，value]保存到map中
    void createMap(Thread t, T firstValue) {
        t.threadLocals = new ThreadLocalMap(this, firstValue);
    }

    // 获取线程t的threadLocals属性，里面保存了所有的ThreadLocal
    ThreadLocalMap getMap(Thread t) {
        return t.threadLocals;
    }
```

### 3.2.3 set 设置ThreadLocal值
使用 set 也可以设置 ThreadLocal 值，使用 set 则会立即生效，不会像 initialValue 那样延迟加载，并且使用 set 后，get 时不会调用 initialValue 方法。

源码如下所示，set 方法将 ThreadLocal this 对象与变量副本都保存到了 map 中，get 时在 map 中可以找到 key==this，直接返回 value 即可，不会执行到 initialValue 方法。

下面代码2中与 initialValue 源码代码1中殊途同归，都是最终将 ThreadLocal  键值对保存到map中，`map.set(this, value)`，却别是 set 直接保存，initialValue 是等待第一个 get 时保存。
```java
// ThreadLocal 源码

    public void set(T value) {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            // 2. key是当前ThreadLocal对象this，
            map.set(this, value);
        else
            createMap(t, value);
    }
    public T get() {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);

        // 调用set后map不为空
        if (map != null) {
            ThreadLocalMap.Entry e = map.getEntry(this);
            // 调用set后map中存在key==this的Entry
            if (e != null) {
                T result = (T)e.value;
                return result;
            }
        }
        // ThreadLocal tl1 调用set后, tl1不会执行到这一步
        return setInitialValue();
    }

```

ThreadLocal 还有一个重要方法就是 remove()，删除线程字段 ThreadLocalMap 中保存的 ThreadLocal 对象，源码如下所示：
```java
    public void remove() {
         ThreadLocalMap m = getMap(Thread.currentThread());
         if (m != null)
             m.remove(this);
     }
```
需要注意的是，如果 remove 之后，调用 get 方法仍会调用 initialValue 进行初始化。

**更加详细完整的 ThreadLocal 源码解析参考文章[ThreadLocal源码完美解读](https://mp.weixin.qq.com/s/mdn5F0Gz9-Ce6-21Z20QxQ)**

## 3.3 ThreadLocalMap 处理哈希冲突

查看 ThreadLocalMap 源码如下所示，可知 ThreadLocalMap 是一个自定义的 Entry 数组。当遇到哈希冲突时，并不是 HashMap 数组加链表的解决方式，
```java
static class ThreadLocalMap {
    // key 为ThreadLocal的弱引用，value为对象强引用
    static class Entry extends WeakReference<ThreadLocal<?>> {
        Object value;

        Entry(ThreadLocal<?> k, Object v) {
            super(k);
            value = v;
        }
    }
    //初始容量，必须为2的幂
    private static final int INITIAL_CAPACITY = 16;

    // Entry表，大小必须为2的幂
    private Entry[] table;

    // 表里entry的个数
    private int size = 0;
    
    // 重新分配表大小的阈值，默认为0
    private int threshold;
```

ThreadLocal需要维持一个最坏2/3的负载因子，对于负载因子相信应该不会陌生，在HashMap中就有这个概念。
ThreadLocal有两个方法用于得到上一个/下一个索引，注意这里实际上是环形意义下的上一个与下一个。

由于ThreadLocalMap使用线性探测法来解决散列冲突，所以实际上Entry[]数组在程序逻辑上是作为一个环形存在的。
```java
// 设置resize阈值以维持最坏2/3的装载因子
private void setThreshold(int len) {
    threshold = len * 2 / 3;
}

// 环形意义的下一个索引
private static int nextIndex(int i, int len) {
    return ((i + 1 < len) ? i + 1 : 0);
}

// 环形意义的上一个索引
private static int prevIndex(int i, int len) {
    return ((i - 1 >= 0) ? i - 1 : len - 1);
}
```
至此，我们已经可以大致勾勒出ThreadLocalMap的内部存储结构。下面是我绘制的示意图。虚线表示弱引用，实线表示强引用。
![ThreadLocalMap的内部存储结构](https://raw.githubusercontent.com/maoturing/PictureBed/master/pic/20200318051855.png)
ThreadLocalMap维护了Entry环形数组，数组中元素Entry的逻辑上的key为某个ThreadLocal对象（实际上是指向该ThreadLocal对象的弱引用），value为代码中该线程往该ThreadLoacl变量实际塞入的值。

## 3.4 内存泄漏

> 什么是内存泄漏？

某个对象不再有用，但是占用的内存却不能被回收。

```java
/**
 * 演示ThreadLocal内存泄漏
 *
 * 循环中对local重复赋值, 导致当前线程中的threadLocals(ThreadLocalMap)中的Entry的key的失去强引用,只剩下Entry的弱引用
 * GC后,可以观察到Entry的key referent为null,弱引用已经被回收.
 * 最后只有value为"text-4"的Entry中的key不为null,因为ThreadLocal local存在对其的强引用
 */
public class ThreadLocalMemoryLeak {

    public static void main(String[] args) {
        // 这样创建ThreadLocal，循环中重复设置set值不会发生内存泄漏，因为是同一个key，修改value而已
        // ThreadLocal<String> local = new ThreadLocal<>();


        ThreadLocal<String> local = null;
        for (int i = 0; i < 5; i++) {
            // 1. 重复创建ThreadLocal，上一次循环创建的ThreadLocal会失去强引用，
            // 是造成内存泄漏的源头
            local = new ThreadLocal<>();
            local.set("text-" + i);

            // 发生gc后会清除弱引用，get会清除ThreadLocalMap中key==null的Entry
            // 因为没有GC，所以ThreadLocalMap中Entry的key都不为null
            System.out.println(local.get());

            // 使用完后移除ThreadLocal，防止内存泄漏
            // local.remove();
        }
        // 获取当前线程，debug查看ThreadLocalMap中的Entry
        Thread thread = Thread.currentThread();

        // debug点1，查看thread.threadLocals.Entry.referent
        // 弱引用对象在gc时被回收，这一步之前，thread.ThreadLocalMap中所有Entry的key(referent)都不为null
        System.gc();

        // debug点2，查看thread.threadLocals属性
        // ThreadLocalMap中前四个Entry key(referent)都为null，最后一次循环[local,text-4]中local存在强引用，不会被回收
        System.out.println();

        // debug点3，前面进行了GC，再次调用get会删除ThreadLocalMap中key==null的Entry
        // 经过debug发现，并没有删除ThreadLocalMap中key==null的Entry，为什么？
        String text = local.get();

        System.out.println(text);
    }
}
```
在上述代码debug点1处设置断点，由于未发生GC，弱引用未被回收，查看thread.threadLocals.Entry.referent如下图所示：
![GC前弱引用未被回收](https://raw.githubusercontent.com/maoturing/PictureBed/master/pic/20200319021111.png)

在上述代码 for 循环中，由于不断重新 `new ThreadLocal()`，导致前四次创建的 ThreadLocal 都失去了强引用，调用`System.gc()`会回收弱引用，也就是说 ThreadLocalMap 中的 5 个 Entry 中 4 个的弱引用 key 会被回收。查看thread.threadLocals.Entry.referent如下图所示：
![GC后弱引用被回收](https://raw.githubusercontent.com/maoturing/PictureBed/master/pic/20200319021400.png)

在代码 1 处，`local` 引用对应下图 Stack 中 `ThreadLocal对象引用-1`，第 2 次循环，第一次创建的 `ThreadLocal对象-1`将失去强引用，对应图中的X号，只剩下来自 ThreadLocalMap->Entry->ThreadLocal对象-1 的一条弱引用，对应图中的虚线。则在 GC 时，ThreadLocal对象-1 被回收。这样一来，ThreadLocalMap 中就会出现 key 为 null 的 Entry，就没有办法访问这些key为null的Entry的value，即发生了内存泄漏。如果当前线程再迟迟不结束的话，这些key为null的Entry的value就会一直存在一条强引用链：Thread Ref -> Thread -> ThreaLocalMap -> Entry -> value永远无法回收，造成内存泄漏。

![ThreadLocal内存泄漏](https://raw.githubusercontent.com/maoturing/PictureBed/master/pic/20200318005427.png)


其实，ThreadLocalMap的设计中已经考虑到这种情况，也加上了一些防护措施：在ThreadLocal的get(),set(),remove()的时候都会清除线程ThreadLocalMap里所有key为null的value。

但是这些被动的预防措施并不能保证不会内存泄漏：
- 使用static的ThreadLocal，延长了ThreadLocal的生命周期，可能导致的内存泄漏。
- 分配使用了ThreadLocal又不再调用get(),set(),remove()方法，那么就会导致内存泄漏。


上面是笔者自己根据描述写的 ThreadLocal 内存泄漏代码示例，下面摘抄了《Java并发编程之美 p339》的 ThreadLocal 内存泄漏代码示例：
```java
/**
 * 线程池中使用ThreadLocal发生内存泄漏
 *
 */
public class ThreadLocalMemoryLeak2 {

    public static final ExecutorService threadPool = Executors.newFixedThreadPool(5);
    public static final ThreadLocal<LocalVariable> threadLocal = new ThreadLocal<>();
    static Set<Thread> threads = new HashSet<>();

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            threadPool.execute(() -> {
                // 使用大对象
                threadLocal.set(new LocalVariable());
                System.out.println("use local variable");

                // 保存线程池中核心线程的引用，用于debug查看ThreadLocalMap中是否保存了对象
                threads.add(Thread.currentThread());

                // 使用完后删除，不执行会造成内存泄漏
//                 threadLocal.remove();
            });
            Thread.sleep(500);
        }
        // 尝试回收线程中ThradLocalMap保存的ThreadLocal的弱引用，因为存在来自静态变量threadLocal的强引用，并不会被回收。
        System.gc();  // jconsole测内存时不要开启，有影响
        
        // 由于没有调用线程池的shutdown方法，线程池中的核心线程并不会退出，进而JVM也不会退出
        // debug点1，查看threads集合中0.threadLocals.Entry.referent和value，
        // 每个线程的ThreadLocalMap中都有value为LocalVariable大对象没被回收，但是key为ThreadLocal，没有被回收
        // 此时jvm进程并不会退出，因为5个线程还存在，jconsole可以监控堆内存的使用量。
        System.out.println("pool executor over");
    }


    static class LocalVariable {
        // long 是64位8B，数组占用内存则为8MB
        private long[] a = new long[1024 * 1024];
    }
}
```
线程池中任务执行完了，由于没有调用线程池shutdown方法，线程池中的核心线程会一直存在，JVM进程也不会退出。下面代码是50个任务，有50个LocalVariable大对象，5个核心线程最后一次调用threadLocal.set(new LocalVariable())，会一直保存在该线程的ThreadLocalMap属性中，所以最后总共有5个LocalVariable大对象没有被回收。
 
使用Jconsle监控堆内存，发现注释remove，最终占用内存81MB，取消注释，最终占用内存40MB，差的40MB正好是5个LocalVariable大对象，每个LocalVariable是一个8MB的long数组。

![注释remove](https://raw.githubusercontent.com/maoturing/PictureBed/master/pic/2c2f162e924bbddb88361c93d7bc7f9.png)

![未注释remove](https://raw.githubusercontent.com/maoturing/PictureBed/master/pic/38db7c691dac0c8acb6e591b873f404.png)

> **问题：** 上面的示例代码中线程池中5个线程一直不结束，一直持有ThreadLocalMap，存在对value的强引用，所以出现内存泄漏。在debug点1设置断点发现，ThreadLocalMap中的key即ThreadLocal对象仍然存在来自静态变量threadLocal 的强引用，所以不会被回收，如下图所示。既然key不为null，能访问到value，何来内存泄漏一说？

![没有被回收的ThreadLocal弱引用](https://raw.githubusercontent.com/maoturing/PictureBed/master/pic/20200319020221.png)


重点
------
以上两个内存泄漏demo都不太合适，**真正内存泄漏的场景是 ThreadLocal 定义在业务类中，线程池定义在其他地方，如果业务对象被回收，则 ThreadLocal 引用会被回收，而线程池引用一直存在。**
- 如果 ThreadLocal 使用强引用，那么 Entry 不会被回收，发生内存泄漏
- 如果 ThreadLocal 使用弱引用，Entry 的弱引用 key 会被回收， value 会在 set、get、rehash等方法中删除 key==null 的 value。



### 3.4.1 为什么使用弱引用
从表面上看内存泄漏的根源在于使用了弱引用。网上的文章大多着重分析ThreadLocal使用了弱引用会导致内存泄漏，但是另一个问题也同样值得思考：**为什么使用弱引用而不是强引用？**


我们先来看看官方文档的说法：

To help deal with very large and long-lived usages, the hash table entries use WeakReferences for keys.

为了应对非常大和长时间的用途，哈希表使用弱引用的 key。

下面我们分两种情况讨论：

- key 使用强引用：引用的ThreadLocal的对象被回收了，但是ThreadLocalMap还持有ThreadLocal的强引用，如果没有手动删除，ThreadLocal不会被回收，导致Entry内存泄漏。

- key 使用弱引用：引用的ThreadLocal的对象被回收了，由于ThreadLocalMap持有ThreadLocal的弱引用，即使没有手动删除，ThreadLocal也会被回收。value在下一次ThreadLocalMap调用set,get，remove的时候会被清除。

比较两种情况，我们可以发现：**由于ThreadLocalMap的生命周期跟Thread一样长**，如果都没有手动删除对应key，都会导致内存泄漏，但是使用弱引用可以多一层保障：弱引用ThreadLocal不会内存泄漏，对应的value在下一次调用 ThreadLocalMap#set，ThreadLocalMap#rehash，ThreadLocalMap#remove的时候会被清除。

因此，**ThreadLocal内存泄漏的根源是**：由于ThreadLocalMap的生命周期跟Thread一样长，如果没有手动删除对应key就会导致内存泄漏，而不是因为弱引用。

当 ThreadLocalMap 需要扩容时会调用 ThreadLocalMap#rehash 方法，rehash 需要对所有元素进行重新 hash 确定位置，在这个过程中，如果发现 Entry 的 key 为null，则清除该Entry，即将 Entry 的 value 置为 null。源码如下所示：
```java
// ThreadLocalMap#rehash 的源码
    private void rehash() {
        expungeStaleEntries();

        if (size >= threshold - threshold / 4)
            resize();
    }

    private void resize() {
        Entry[] oldTab = table;
        int oldLen = oldTab.length;
        int newLen = oldLen * 2;
        Entry[] newTab = new Entry[newLen];
        int count = 0;

        for (int j = 0; j < oldLen; ++j) {
            Entry e = oldTab[j];
            if (e != null) {
                ThreadLocal<?> k = e.get();
                if (k == null) {
                    // Entry不为空，Entry的key为空，则需要清除该Entry
                    e.value = null; // Help the GC
                } else {
                    int h = k.threadLocalHashCode & (newLen - 1);
                    while (newTab[h] != null)
                        h = nextIndex(h, newLen);
                    newTab[h] = e;
                    count++;
                }
            }
        }

        setThreshold(newLen);
        size = count;
        table = newTab;
    }

```


下面是 ThreadLocalMap#set 的源码，这个方法会在 ThreadLocal#set 中被调用，用于保存 ThreadLocal 键值对到 ThreadLocalMap。
```java
// ThreadLocalMap#set 的源码
    private void set(ThreadLocal<?> key, Object value) {

        Entry[] tab = table;
        int len = tab.length;
        // 根据hash值得到在map中的位置
        int i = key.threadLocalHashCode & (len-1);

        // 依次查找元素，线性弹测法确定hash位置
        for (Entry e = tab[i];
                e != null;
                e = tab[i = nextIndex(i, len)]) {
            ThreadLocal<?> k = e.get();

            if (k == key) {
                // 如果k==key，则说明找到了对应的Entry
                e.value = value;
                return;
            }
            // Entry不为空，Entry的key为空，则需要清除该Entry
            if (k == null) {
                replaceStaleEntry(key, value, i);
                return;
            }
        }

        tab[i] = new Entry(key, value);
        int sz = ++size;
        if (!cleanSomeSlots(i, sz) && sz >= threshold)
            rehash();
    }

    private boolean cleanSomeSlots(int i, int n) {
        boolean removed = false;
        Entry[] tab = table;
        int len = tab.length;
        do {
            i = nextIndex(i, len);
            Entry e = tab[i];
            // Entry不为空，Entry的key为空，则需要清除该Entry
            if (e != null && e.get() == null) {
                n = len;
                removed = true;
                i = expungeStaleEntry(i);
            }
        } while ( (n >>>= 1) != 0);
        return removed;
    }
```

> 面试题： ThreadLocalMap什么时候会被清除 key==null 的 Entry？

调用 set，get，remove，rehash方法时会清楚 key==null 的 Entry，防止内存泄漏

### 3.4.2 ThreadLocal 最佳实践
1. **使用完一点 remove()**，综合上面的分析，我们可以理解ThreadLocal内存泄漏的前因后果，那么怎么避免内存泄漏呢？

- **每次使用完ThreadLocal，都调用它的remove()方法，清除数据。**
- **使用完ThreadLocal，当前线程 Thread 也随之运行结束**

在使用线程池的情况下，没有及时清理ThreadLocal，不仅是内存泄漏的问题，更严重的是可能导致业务逻辑出现问题。所以，使用ThreadLocal就跟加锁完要解锁一样，用完就清理。


2. **空指针异常**，ThreadLocal 没有设置值直接 get() 会返回null，但是操作不当可能出现空指针异常。
```java
public class ThreadLocalNPE {
    ThreadLocal<Long> longThreadLocal = new ThreadLocal<>();

    // get() 方法返回值是Long类型，拆箱转换为long类型是Long.longValue()，如果返回值为null则会发现空指针异常
    // 解决办法：将getValue返回值修改为Long
    public long getValue() {
        // 可能出现空指针异常
        return longThreadLocal.get();
    }

    public static void main(String[] args) {
        ThreadLocalNPE threadLocal = new ThreadLocalNPE();
        System.out.println(threadLocal.getValue());
    }
}
```

2. **共享对象**，如果每个线程中 ThreadLocal.set() 的参数对象本身就是线程共享的对象，比如 static 对象，那么多个线程的 ThreadLocal.get() 取得的还是同一个共享对象，存在线程安全问题


> 彩蛋：ThreadLocal 要 set 更要 remove；线程池要 execute 更要 shutdown
> 
# 待补充
关于ThreadLocal的面试题

弱引用，根据码出高效补充，软引用可以做缓存

延迟加载与单例和lambda联系

完整的源码分析，参考[ThreadLocal源码完美解读](https://mp.weixin.qq.com/s/mdn5F0Gz9-Ce6-21Z20QxQ)

Spring注解日志记录与ThreadLocal https://www.cnblogs.com/songzehao/p/11000723.html

[慕课网 ThreadLocal 教学视频学习笔记](https://mp.weixin.qq.com/s/RfddBhIQPRTR4G9UXrnBpA)

[ThreadLocal - 求老仙奶我不到P10](https://www.imooc.com/learn/1217)




# 参考文档与推荐阅读

1. [ThreadLocal源码完美解读](https://mp.weixin.qq.com/s/mdn5F0Gz9-Ce6-21Z20QxQ)，网上最详细专业全面的源码解读，https://www.cnblogs.com/micrari/p/6790229.html#4524899
2. [深入分析 ThreadLocal 内存泄漏问题](https://mp.weixin.qq.com/s/VeL9tMavp4ppv3j2w9hwVg) 有问题，get不会清楚key==null的Entry
3. [Java并发编程入门与高并发面试](https://coding.imooc.com/class/chapter/195.html)  补充笔记
4. [ThreadLocal 详解与源码解析 - 黑马](https://www.bilibili.com/video/BV1N741127FH)  补充笔记