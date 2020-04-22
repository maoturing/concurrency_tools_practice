<!-- TOC -->

- [5 原子类](#5-原子类)
    - [5.1 AtomicInteger](#51-atomicinteger)
    - [LongAdder](#longadder)
        - [LongAdder 源码分析](#longadder-源码分析)
        - [LongAdder 与 AtomicLong 的适用场景](#longadder-与-atomiclong-的适用场景)
    - [LongAccumulator](#longaccumulator)

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
从上面的分析来看是不行的，因为AtomicLong提供了很多cas方法，例如getAndIncrement、getAndDecrement等，使用起来非常的灵活，而LongAdder只有add和sum，使用起来比较受限。
优点:由于 JVM 会将 64位的double,long 型变量的读操作分为两次32位的读操作,所以低并发保持了 AtomicLong性能,高并发下热点数据被 hash 到多个 Cell,有限分离,通过分散提升了并行度
但统计时有数据更新,也可能会出现数据误差,但高并发场景有限使用此类，低时还是可以继续 AtomicLong。 

## LongAccumulator

LongAdder 类是 LongAccumulator 的一个特例，LongAccumulator 功能更加强大，可以传入计算函数，也可以指定初始值，代码示例

下面两行代码实现的功能是一样的，都可以实现线程安全的累加。
```java
    LongAdder adder = new LongAdder();
    adder.increment();

    LongAccumulator accumulator = new LongAccumulator((x, y) -> x + y, 0);
    accumulator.accumulate(1);
```

