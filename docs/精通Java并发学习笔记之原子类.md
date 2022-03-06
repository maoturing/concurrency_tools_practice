<!-- TOC -->

- [5 原子类](#5-原子类)
    - [5.1 AtomicInteger](#51-atomicinteger)
        - [AtomicInteger 与 synchronized](#atomicinteger-与-synchronized)
    - [LongAdder](#longadder)
        - [LongAdder 源码分析](#longadder-源码分析)
        - [LongAdder 与 AtomicLong 的适用场景](#longadder-与-atomiclong-的适用场景)
    - [LongAccumulator](#longaccumulator)
- [6 CAS](#6-cas)
        - [CAS 实现原子操作三大问题](#cas-实现原子操作三大问题)

<!-- /TOC -->

# 5 原子类

JUC 包中提供了许多原子性操作类，这些类都是使用非阻塞算法CAS实现的，原子类的作用和锁类似，都是为了保证并发情况下线程安全。相比使用锁实现原子操作性能更好具有以下优点：
- 粒度更细：原子变量可以把竞争范围缩小到变量级别，通常锁的粒度都要大于原子变量的粒度。
- 效率更高：CAS相比切换到内核态挂起唤醒线程效率更高，除了高度竞争的情况原子类效率更高。

常见 6 种原子类如下所示：

| 原子类型                 | 举例                                                         | 作用 |
| ------------------------ | ------------------------------------------------------------ | ---- |
| Atomic-基本类型          | AtomicInteger，AtomicLong，AtomicBoolean                     |      |
| Atomic-Array数组类型     | AtomicIntegerArray，AtomicLongArray，AtomicBooleanArray      |      |
| Atomic-Reference引用类型 | AtomicReference、AtomicStampedReference、AtomicMarkableReference |      |
| Atomic-FieldUpdater升级  | AtomicIntegerFieldUpdater，AtomicLongFieldUpdater，AtomicReferenceFieldUpdater |      |
| Adder 累加器             | LongAdder、DoubleAdder                                       |      |
| Accumlator 累加器        | LongAccumlator、DoubleAccumlator                             |      |

## 5.1 AtomicInteger

AtomicInteger 的使用示例如下所示，对于基本类型如果需要保证线程安全，我们可以使用 AtomicInteger 来代替 synchronized 和 Lock，使用更加简洁优雅，也保证了线程安全。
```java
public class AtomicintegerDemo {
    public volatile int num = 0;
    private AtomicInteger atomicInteger = new AtomicInteger(0);


    public static void main(String[] args) throws InterruptedException {
        AtomicintegerDemo dmeo = new AtomicintegerDemo();
        // 两个线程各执行1000次累加，期望结果是得到2000
        Thread t1 = new Thread(() -> dmeo.increment());
        Thread t2 = new Thread(() -> dmeo.increment());
        t1.start();
        t2.start();
        // 等待两个线程执行结束
        t1.join();
        t2.join();

        // 打印普通变量的累加结果
        System.out.println(dmeo.num);
        // 打印原子变量的累加结果
        System.out.println(dmeo.atomicInteger.get());
    }

    // 基础类型+1
    public void incrementBasic() {
        num++;
    }

    // 原子类型+1
    public void incrementAtomic() {
        atomicInteger.getAndIncrement();
    }

    // 累加1000次
    public void increment() {
        for (int i = 0; i < 1000; i++) {
            incrementBasic();
            incrementAtomic();
        }
    }  
}
```

AtomicInteger 常见方法如下所示，代码示例见Github：
- `set(int value)`  设置值
- `get()`  获取当前值
- `getAndSet(int newValue)`  获取当前值，并设置新值
- `getAndIncrement()`  获取当前值，并自增+1
- `getAndDecrement()`  获取当前值，并自减-1
- `getAndAdd(int value)`  获取当前值，并加上值value
- `incrementAndGet()`  先自增+1，再返回自增后的值
- `compareAndSet(int expect, int update)`  使用CAS方式修改值，修改成功返回true，修改失败返回false

### AtomicInteger 与 synchronized 
只有 synchronized 中的自适应自旋锁，才会自旋一定次数后将线程挂起，即升级为重量级锁。而 AtomicInteger 会死循环CAS直至成功，所以高并发环境下 synchronized 效率会高于AtomicInteger，这也是LongAdder诞生的原因。




常见6种原子类
原子类使用

数组原子类

引用原子类

升级为原子类

## LongAdder

AtomicLong 通过 CAS 提供了非阻塞的原子操作，相比使用阻塞算法的同步器性能已经很好了，但是使用AtomicLong时，在高并发环境下大量线程会去竞争更新同一个原子变量，但是由于同时只会有一个线程的CAS操作会成功，这就导致大量线程竞争失败后，会进行死循环不断自旋尝试CAS操作，这样会浪费CPU资源。

针对高并发环境下CAS操作浪费CPU资源之外，AtomicLong 还有一个缺点就是更新数据前需要从主存获取数据，更新数据后需要刷新数据到主存。如下图所示，thread-1 运行在 core-1 上，修改变量 ctr 后，需要将 ctr 从本地内存刷新flush到主存；thread-2 运行在 core-2 上，修改变量 ctr 前，需要从主存获取 ctr 的最新数据刷新refresh到本地内存。
（CAS涉及到预期值，主内存值，更新值 。 当且仅当预期值==主内存值时候，才会将主内存值更新为更新值 。 ）
![AtomicLong 的数据更新](https://raw.githubusercontent.com/maoturing/PictureBed/master/pic/20200422191452.png)


**针对高并发环境下CAS操作浪费CPU资源，和每次更新都需要刷新到主存的缺点**，JDK8中提供了一个**原子自增自减类`LongAdder`**。

AtomicLong的性能瓶颈是多个线程竞争一个变量的更新导致的，**LongAdder的思路就是空间换时间，每个线程保存一份变量的副本进行自增自减操作**，这样就避免了多个线程竞争，在最后获取结果时，再将这多个副本变量相加即可得到结果。

![20200422230439](https://raw.githubusercontent.com/maoturing/PictureBed/master/pic/20200422230439.png)

如下图所示，LongAdder 会在每个线程保存一份变量 ctr 的副本，就能避免多个线程CAS竞争，也不需要频繁刷新数据到主存。
![LongAdder 的数据更新](https://raw.githubusercontent.com/maoturing/PictureBed/master/pic/20200422192246.png)

需要执行 1w 个任务，每个任务的操作是累加 1w 次，这些任务由 20 个线程执行，
[LongAdder示例代码如下：](https://github.com/maoturing/concurrency_tools_practice/blob/master/src/main/java/atomic/LongAdderDemo.java)

```java
public class LongAdderDemo {

    public static void main(String[] args) throws InterruptedException {
        // 计数器counter
        LongAdder counter = new LongAdder();
        ExecutorService service = Executors.newFixedThreadPool(20);
        long start = System.currentTimeMillis();

        // 1w个累加任务，每个累加任务执行1w次累加操作
        for (int i = 0; i < 10000; i++) {
            service.submit(new Task(counter));
        }
        service.shutdown();
        // 等待任务执行完毕
        while (!service.isTerminated()) {
        }

        long end = System.currentTimeMillis();

        // 打印计数器结果和耗时
        System.out.println(counter.sum());
        System.out.println("LongAdder耗时：" + (end - start));
    }

    private static class Task implements Runnable {

        private LongAdder counter;

        public Task(LongAdder counter) {
            this.counter = counter;
        }

        // 累加任务，任务内容是累加1w次
        @Override
        public void run() {
            for (int i = 0; i < 10000; i++) {
                counter.increment();
            }
        }
    }
}

```
上面代码使用 LongAdder 作为计数器耗时为 AtomicLong 的十分之一。如果使用 AtomicLong，每次自增 increment() 操作都需要修改值并刷新到主存，自增失败的线程需要也会进行自旋尝试，浪费CPU资源。而使用 LongAdder 则会修改每个线程的 counter 变量副本，在最后使用`sum()`方法求和即可。


### LongAdder 源码分析

LongAdder#sum() 方法的源码如下所示，是对 Cell 数组的所有值求和，再与 base 相加得到LongAdder 的值。由于求和时没有对 Cell 数组进行加锁，所以在求和操作时可能有线程对Cell 值进行了修改，因此在上面的示例代码中，我们是等线程执行完毕才进行的求和`sum()`操作。

```java
    // 返回LongAdder的值
    public long sum() {
        Cell[] as = cells; 
        Cell a;
        long sum = base;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    // 对base和数组所有值求和
                    sum += a.value;
            }
        }
        return sum;
    }

    // 等价于sum求和操作
    public long longValue() {
        return sum();
    }
```

下面是 LongAdder#add 的源码，
在代码1处，分析如下：

如果以下两种条件则继续执行if内的语句
  1. cells数组不为null（不存在争用的时候，cells数组一定为null，一旦对base的cas操作失败，才会初始化cells数组）
  2. 如果cells数组为null，如果casBase执行成功，则直接返回；如果casBase方法执行失败（casBase失败，说明第一次争用冲突产生，需要对cells数组初始化）进入if内；casBase方法很简单，就是通过UNSAFE类的cas设置成员变量base的值为base+要累加的值。casBase执行成功的前提是无竞争，这时候cells数组还没有用到为null，可见在无竞争的情况下是类似于AtomticInteger处理方式，使用cas做累加。

在代码2处，分析如下：
  1. as == null ： cells数组未被初始化，成立则直接进入if执行cell初始化
  2. (m = as.length - 1) < 0： cells数组的长度为0，条件1与2都代表cells数组没有被初始化成功，初始化成功的cells数组长度为2；
  3. (a = as[getProbe() & m]) == null ：如果cells被初始化，且它的长度不为0，则通过getProbe方法获取当前线程Thread的threadLocalRandomProbe变量的值，初始为0，然后执行threadLocalRandomProbe&(cells.length-1 ),相当于m%cells.length；如果cells[threadLocalRandomProbe%cells.length]的位置为null，这说明这个位置从来没有线程做过累加，需要进入if继续执行，在这个位置创建一个新的Cell对象；
  4. !(uncontended = a.cas(v = a.value, v + x))：尝试对cells[threadLocalRandomProbe%cells.length]位置的Cell对象中的value值做累加操作,并返回操作结果,如果失败了则进入if，重新计算一个threadLocalRandomProbe；

在代码3处，即进入if语句执行longAccumulate方法,有三种情况
1. 前两个条件代表cells没有初始化，
2. 第三个条件指当前线程hash到的cells数组中的位置还没有其它线程做过累加操作，
3. 第四个条件代表产生了冲突,uncontended=false

```java
    public void add(long x) {
        Cell[] as; 
        long b, v; 
        int m; 
        Cell a;

        // 代码1，cells不为null时使用CAS操作在base上相加，即casBase
        if ((as = cells) != null || !casBase(b = base, b + x)) {
            // 如果cells不为null，或者CAS操作失败了，则执行下面操作

            // uncontended判断cells数组中，当前线程要做cas累加操作的某个元素是否不存在争用，
            // 如果cas失败则存在争用；false代表存在争用，true代表不存在争用。
            boolean uncontended = true;
            // 代码2，
            if (as == null || (m = as.length - 1) < 0 ||   
                (a = as[getProbe() & m]) == null ||
                !(uncontended = a.cas(v = a.value, v + x)))

                // 代码3
                longAccumulate(x, null, uncontended);
        }
    }

    /**
     * Equivalent to {@code add(1)}.
     */
    public void increment() {
        add(1L);
    }
```

// 补充longAccumulate源码分析，参考Java并发编程之美


### LongAdder 与 AtomicLong 的适用场景
从上面的分析来看是不行的，因为AtomicLong提供了很多cas方法，例如getAndIncrement、getAndDecrement等，使用起来非常的灵活，而LongAdder只有add和sum，适合的是统计求和计数的场景，场景比较单一。
优点:由于 JVM 会将 64位的double,long 型变量的读操作分为两次32位的读操作,所以低并发保持了 AtomicLong性能,高并发下热点数据被 hash 到多个 Cell,有限分离,通过分散提升了并行度
但统计时有数据更新,也可能会出现数据误差,但高并发场景有限使用此类，低时还是可以继续 AtomicLong。 

## LongAccumulator

LongAdder 类是 LongAccumulator 的一个特例，LongAccumulator 功能更加强大，可以传入计算函数，也可以指定初始值，[查看LongAccumulator示例代码](https://github.com/maoturing/concurrency_tools_practice/blob/master/src/main/java/atomic/LongAccumulatorDemo.java)。
```java
public class LongAccumulatorDemo {
    public static void main(String[] args) {
        // 累加器，初始值为100，传入函数是表示对传入数值和当前值进行的运算
        LongAccumulator longAccumulator = new LongAccumulator((x, y) -> x + y, 100);
        // 传入值为1，根据传入函数，是将1与当前值相加
        longAccumulator.accumulate(1);
        longAccumulator.accumulate(2);

        System.out.println(longAccumulator.get());
    }
}
```
LongAdder#add 方法与 LongAccumulator#accumulate 方法最终都调用的 Striped64#longAccumulate 方法，区别是LongAdder 使用默认的相加操作，而 LongAccumulator 会传入自定义的计算函数。

下面是 LongAccumulator 的源码：
```java
public class LongAccumulator extends Striped64 implements Serializable {

    private final LongBinaryOperator function;
    private final long identity;

    /**
     * @param accumulatorFunction 对传入值与当前值做的运算
     * @param identity identity 初始值
     */
    public LongAccumulator(LongBinaryOperator accumulatorFunction,
                           long identity) {
        // 保存自定义的运算规则
        this.function = accumulatorFunction;
        base = this.identity = identity;
    }


    public void accumulate(long x) {
        Cell[] as; long b, v, r; int m; Cell a;
        if ((as = cells) != null ||
            (r = function.applyAsLong(b = base, x)) != b && !casBase(b, r)) {
            boolean uncontended = true;
            if (as == null || (m = as.length - 1) < 0 ||
                (a = as[getProbe() & m]) == null ||
                !(uncontended =
                  (r = function.applyAsLong(v = a.value, x)) == v ||
                  a.cas(v, r)))
                
                // 最终调用，与LongAdder不同的是需要传入自定义的函数function
                longAccumulate(x, function, uncontended);
        }
    }
```




下面两行代码实现的功能是一样的，都可以实现线程安全的累加。
```java
    LongAdder adder = new LongAdder();
    adder.increment();

    LongAccumulator accumulator = new LongAccumulator((x, y) -> x + y, 0);
    accumulator.accumulate(1);
```

# 6 CAS 

在Java中可以通过锁或CAS的方式来实现原子操作，JVM中的CAS操作是使用处理器提供的CMPXCHG指令实现的。自旋CAS实现的基本思路就是循环进行CAS操作直至成功为止。

### CAS 实现原子操作三大问题

1. ABA问题
   
> 什么是ABA问题?
> 
因为CAS需要在操作值得时候，检查值有没有发生变化，如果没有发生变化则更新，但是如果一个值原来是A、变成了B、又变成了A，那么使用CAS进行检查时会发现它的值没有发生变化，但实际上却变化了。

解决ABA问题
- 使用版本号

ABA问题的解决思路是使用版本号，每次变量更新的时候版本号加1，那么A->B->A就会变成1A->2B->3A

- jdk自带原子变量

从jdk1.5开始，jdk的Atomic包里就提供了一个类AtomicStampedReference来解决ABA问题，这个类中的compareAndSet方法的作用就是首先检查当前引用是否等于预期引用，并且检查当前标志是否等于预期标志，如果全部相等，则以原子方式将该引用和该标志的值更新为指定的新值

```java
/**
     * 如果当前引用等于预期引用并且当前标志等于预期标志
     * 则以原子方式将该引用和该标志的值设置为给定新值
     *
     * @param expectedReference 预期引用值
     * @param newReference 新的引用值
     * @param expectedStamp 预期标记值
     * @param newStamp 新标记值
     * @return {@code true} if successful
     */
    public boolean compareAndSet(V   expectedReference,
                                 V   newReference,
                                 int expectedStamp,
                                 int newStamp) {
        Pair<V> current = pair;
        return
        #预期引用==当前引用
            expectedReference == current.reference &&
            #预期标志==当前标志
            expectedStamp == current.stamp &&
            #新引用==当前引用 并且 新标志==当前标志
            ((newReference == current.reference &&
              newStamp == current.stamp) ||
              #原子更新值
             casPair(current, Pair.of(newReference, newStamp)));
    }
```
2. 循环时间长开销大
自旋CAS如果长时间不成功，会给CPU带来非常大的执行开销。如果jvm能支持处理器提供的pause指令，那么效率会有一定的提升。pause指令有两个作用：

第一，它可以延迟流水线执行指令（de-pipeline），使CPU不会消耗过多的执行资源，延迟的时间取决于具体实现的版本，在一些处理器上延迟时间是零。

第二，它可以避免在退出循环的时候因内存顺序冲突（Memory Order Violation）而引起CPU流水线被清空（CPU Pipeline Flush），从而提高CPU的执行效率。

3. 只能保证一个共享变量的原子操作
当对一个共享变量执行操作时，我们可以使用循环CAS的方式来保证原子操作，但是多个共享变量操作时，循环CAS就无法保证操作的原子性，这个时候就可以用锁。还有一个方法，就是把多个共享变量合并成一个共享变量来操作。比如，有两个共享变量i=2,j=a合并一下ij=2a，然后用CAS来操作ij。从java1.5开始，JDK提供了AtomicReference类来保证引用对象之间的原子性，就可以把多个变量放在一个对象里来进行CAS操作。





CAS适用场景

除了偏向锁，JVM实现锁的方式都使用了循环CAS。即当一个线程进入同步块时使用循环CAS的方式来获取锁，退出同步块时使用循环CAS的方式释放锁。


**synchronized中的轻量级锁自旋锁才会尝试10次CAS然后升级为重量级锁，而AtomicInteger 中的CAS会真的一直循环直至CAS成功，所以在高并发环境下建议使用LongAdder代替AtomicInteger。**




CAS 的源码实现 https://www.jianshu.com/p/c8e9bce8b3c6

LOCK cmpxchg

https://github.com/AdoptOpenJDK/openjdk-jdk8u/blob/master/hotspot/src/os_cpu/windows_x86/vm/atomic_windows_x86.inline.hpp    216行

并发编程艺术 p53