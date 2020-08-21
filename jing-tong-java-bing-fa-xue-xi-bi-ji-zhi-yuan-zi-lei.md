# 精通Java并发学习笔记之原子类

* [5 原子类](jing-tong-java-bing-fa-xue-xi-bi-ji-zhi-yuan-zi-lei.md#5-原子类)
  * [5.1 AtomicInteger](jing-tong-java-bing-fa-xue-xi-bi-ji-zhi-yuan-zi-lei.md#51-atomicinteger)
  * [LongAdder](jing-tong-java-bing-fa-xue-xi-bi-ji-zhi-yuan-zi-lei.md#longadder)

## 5 原子类

JUC 包中提供了许多原子性操作类，这些类都是使用非阻塞算法CAS实现的，原子类的作用和锁类似，都是为了保证并发情况下线程安全。相比使用锁实现原子操作性能更好具有以下优点：

* 粒度更细：原子变量可以把竞争范围缩小到变量级别，通常锁的粒度都要大于原子变量的粒度。
* 效率更高：CAS相比切换到内核态挂起唤醒线程效率更高，除了高度竞争的情况原子类效率更高。

常见 6 种原子类如下所示：

| 原子类型 | 举例 | 作用 |
| :--- | :--- | :--- |
| Atomic-基本类型 | AtomicInteger，AtomicLong，AtomicBoolean |  |
| Atomic-Array数组类型 | AtomicIntegerArray，AtomicLongArray，AtomicBooleanArray |  |
| Atomic-Reference引用类型 | AtomicReference、AtomicStampedReference、AtomicMarkableReference |  |
| Atomic-FieldUpdater升级 | AtomicIntegerFieldUpdater，AtomicLongFieldUpdater，AtomicReferenceFieldUpdater |  |
| Adder 累加器 | LongAdder、DoubleAdder |  |
| Accumlator 累加器 | LongAccumlator、DoubleAccumlator |  |

### 5.1 AtomicInteger

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

* `set(int value)`  设置值
* `get()`  获取当前值
* `getAndSet(int newValue)`  获取当前值，并设置新值
* `getAndIncrement()`  获取当前值，并自增+1
* `getAndDecrement()`  获取当前值，并自减-1
* `getAndAdd(int value)`  获取当前值，并加上值value
* `incrementAndGet()`  先自增+1，再返回自增后的值

常见6种原子类 原子类使用

数组原子类

引用原子类

升级为原子类

### LongAdder

AtomicLong 通过 CAS 提供了非阻塞的原子操作，相比使用阻塞算法的同步器性能已经很好了，但是使用AtomicLong时，在高并发环境下大量线程会去竞争更新同一个原子变量，但是由于同时只会有一个线程的CAS操作会成功，这就导致大量线程竞争失败后，会进行死循环不断自旋尝试CAS操作，这样会浪费CPU资源。

**针对高并发环境下CAS操作浪费CPU资源的缺点**，JDK8中提供了一个**原子自增自减类`LongAdder`**

```java
    public long sum() {
        Cell[] as = cells; 
        Cell a;
        long sum = base;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    sum += a.value;
            }
        }
        return sum;
    }
```

```java
    public void add(long x) {
        Cell[] as; 
        long b, v; 
        int m; 
        Cell a;
        if ((as = cells) != null || !casBase(b = base, b + x)) {
            boolean uncontended = true;
            if (as == null || (m = as.length - 1) < 0 ||
                (a = as[getProbe() & m]) == null ||
                !(uncontended = a.cas(v = a.value, v + x)))
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

累加器

