# 精通Java并发学习笔记之锁

* [4 千变万化的锁](jing-tong-java-bing-fa-xue-xi-bi-ji-zhi-suo.md#4-千变万化的锁)
  * [4.1 Lock 接口](jing-tong-java-bing-fa-xue-xi-bi-ji-zhi-suo.md#41-lock-接口)
  * [4.2 Lock 常用 5 个方法](jing-tong-java-bing-fa-xue-xi-bi-ji-zhi-suo.md#42-lock-常用-5-个方法)
  * [4.3 Lock 的可见性](jing-tong-java-bing-fa-xue-xi-bi-ji-zhi-suo.md#43-lock-的可见性)
  * [4.4 锁的分类](jing-tong-java-bing-fa-xue-xi-bi-ji-zhi-suo.md#44-锁的分类)
    * [4.4.1 乐观锁和悲观锁](jing-tong-java-bing-fa-xue-xi-bi-ji-zhi-suo.md#441-乐观锁和悲观锁)
    * [4.4.2 可重入锁与非可重入锁](jing-tong-java-bing-fa-xue-xi-bi-ji-zhi-suo.md#442-可重入锁与非可重入锁)
    * [4.4.3 公平锁与非公平锁](jing-tong-java-bing-fa-xue-xi-bi-ji-zhi-suo.md#443-公平锁与非公平锁)
    * [4.4.3 公平锁与非公平锁](jing-tong-java-bing-fa-xue-xi-bi-ji-zhi-suo.md#443-公平锁与非公平锁)
    * [4.4.4 共享锁与排它锁](jing-tong-java-bing-fa-xue-xi-bi-ji-zhi-suo.md#444-共享锁与排它锁)
    * [4.4.5 自旋锁与阻塞锁](jing-tong-java-bing-fa-xue-xi-bi-ji-zhi-suo.md#445-自旋锁与阻塞锁)
    * [4.4.6 可中断锁与不可中断锁](jing-tong-java-bing-fa-xue-xi-bi-ji-zhi-suo.md#446-可中断锁与不可中断锁)
  * [4.5 锁优化](jing-tong-java-bing-fa-xue-xi-bi-ji-zhi-suo.md#45-锁优化)
    * [4.5.1 自适应自旋锁](jing-tong-java-bing-fa-xue-xi-bi-ji-zhi-suo.md#451-自适应自旋锁)
    * [4.5.2 锁消除](jing-tong-java-bing-fa-xue-xi-bi-ji-zhi-suo.md#452-锁消除)
    * [4.5.3 锁粗化](jing-tong-java-bing-fa-xue-xi-bi-ji-zhi-suo.md#453-锁粗化)
    * [4.5.4 重量级锁](jing-tong-java-bing-fa-xue-xi-bi-ji-zhi-suo.md#454-重量级锁)
    * [4.5.4 轻量级锁](jing-tong-java-bing-fa-xue-xi-bi-ji-zhi-suo.md#454-轻量级锁)
    * [4.5.5 偏向锁](jing-tong-java-bing-fa-xue-xi-bi-ji-zhi-suo.md#455-偏向锁)
  * [4.6 ReentrantLock](jing-tong-java-bing-fa-xue-xi-bi-ji-zhi-suo.md#46-reentrantlock)
  * [4.7 ReentrantReadWriteLock](jing-tong-java-bing-fa-xue-xi-bi-ji-zhi-suo.md#47-reentrantreadwritelock)
* [推荐阅读](jing-tong-java-bing-fa-xue-xi-bi-ji-zhi-suo.md#推荐阅读)
* [参考文档](jing-tong-java-bing-fa-xue-xi-bi-ji-zhi-suo.md#参考文档)

## 4 千变万化的锁

### 4.1 Lock 接口

锁是一种工具，用于控制对**共享资源**的访问，我们已经有了 synchronized 锁，为什么还需要 Lock 锁呢？

synchronized 锁存在以下问题： 1. 效率低：试图获取锁时不能设定超时，会一直等待 2. 不够灵活：加锁释放锁时机单一 3. 无法知道是否已经获取了锁

### 4.2 Lock 常用 5 个方法

在Lock 中声明了 4 个方法来获取锁：

* `lock()`  获取锁，如果锁被其他线程获取，则进行等待。Lock 不会像synchronized 自动释放锁，即使发生异常，也能争取释放锁，Lock 需要在 finally 中释放锁，以保证在发生异常时锁被正确释放。`lock()`方法不能被中断，获取不到锁则会一直等待，一旦陷入死锁`lock()`会进入永久等待。
* `unlock()` 释放锁，如果当前线程没有持有该锁调用该方法会抛出 IllegalMonitorStateException 异常

  \`\`\`java public class LockDemo {

  public static final Lock lock = new ReentrantLock\(\);

  public static void main\(String\[\] args\) throws InterruptedException { LockDemo lockDemo = new LockDemo\(\); // 只有第一个线程能获取锁，另一个线程会一直等待 // new Thread\(\(\) -&gt; lockDemo.testLock\(\)\).start\(\); // new Thread\(\(\) -&gt; lockDemo.testLock\(\)\).start\(\);

```text
    // 两个线程都能获取到锁
    new Thread(() -> lockDemo.testLock2()).start();
    new Thread(() -> lockDemo.testLock2()).start();
}

// 发生异常没有正确释放锁，线程2会一直等待获取锁
public void testLock() {
    lock.lock();
    System.out.println("已经获取到了锁");
    // 模拟异常，看能否正确释放锁
    int a = 1 / 0;
    lock.unlock();
}

// 在finally中释放锁，即使发生异常，也能正确释放
public void testLock2() {
    lock.lock();
    try {
        System.out.println("已经获取到了锁");
        // 模拟异常，看能否正确释放锁
        int a = 1 / 0;
    } finally {
        // 在finally中释放锁
        lock.unlock();
    }
}
```

}

```text
- `tryLock()`  尝试获取锁，如果当前锁没有被其他线程持有，则获取成功返回true，否则返回false。该方法不会引起当先线程阻塞，非公平锁。
- `tryLock(long time)`  尝试获取锁，获取成功返回true，如果超时时间到仍没有获取到锁则返回false。相比`lock()`更加强大，我们可以根据是否能够获取到锁来决定后续行为。
- `lockInterruptibly()` 尝试获取锁，等待锁过程中允许被中断，可以被`thread.interrupt()`中断，中断后抛出InterruptedException。


## 4.3 Lock 的可见性

Monitor 锁 Happen-Before 原则（synchronized和Lock）：对一个锁的解锁，对于后续其他线程同一个锁的加锁可见，这里的“后续”指的是时间上的先后顺序，又叫管程锁定原则。

Java编译器会在生成指令序列时在适当位置插入内存屏障指令来禁止处理器重排序，来保证可见性。


## 4.4 锁的分类
锁有多种分类方法，根据不同的分类方法，相同的一个锁可能属于不同的类别，比如 ReentrantLock 既是互斥锁，又是可重入锁。
根据不同的分类标准，锁大致可以分为以下 6 种：
![锁的分类](https://raw.githubusercontent.com/maoturing/PictureBed/master/pic/20200324172908.png)

### 4.4.1 乐观锁和悲观锁
根据线程要不要锁住同步资源，锁可以分为乐观锁与悲观锁。

> 为什么诞生乐观锁？

悲观锁又称互斥同步锁，具有以下缺点：
1. 阻塞和唤醒带来的性能劣势，用户态核心态切换，检查是否有阻塞线程需要被唤醒
2. 可能永久阻塞，如果持有锁的线程被永久阻塞，比如死锁等，那么等待锁的线程将永远不会被执行
3. 优先级反转

悲观锁指对数据被外界修改持悲观态度，认为数据很容易被其他线程修改，所以在数据处理前需要对数据进行加锁。Java 中典型的悲观锁是 synchronized 和 Lock。

乐观锁认为修改数据在一般情况下不会造成冲突，所以在修改记录前不会加锁，但在数据提交更新时，才会对数据冲突与否进行检测，检查我修改数据期间，有没有其他线程修改过，一般通过加 version 字段或 CAS 算法实现。Java中的典型乐观锁是原子类和并发容器。自旋锁（CAS）是乐观锁的一种实现方式。

在数据库中就有对乐观锁的典型应用：要更新一条数据，首先查询数据的version:`select * from table;`
然后更新语句，update set num=2，version=version+1 where version=1 and id=5,如果数据没有被其他人修改，则version与查询数据时的version一直都为1，则可以修改成功，并返回version=2，如果被其他人修改了，则重新查询和更新数据。

**开销对比：**
1. 悲观锁的原始开销要高于乐观锁，但是特点是一劳永逸。适合资源竞争激烈的情况，持有锁时间长的情况
2. 乐观锁如果自旋时间很长或不停重试，消耗的资源也会越来越多。适用于资源竞争不激烈的情况

**适用场景：**

悲观锁，适合并发写入多，持有锁时间较长的情况，如临界区有IO操作，临界区代码复杂循环量大，临界区竞争激烈等情况
乐观锁，适合并发写入少，不加锁能提高效率。如少写多读数据的场景，数据改变概率小，自旋次数少。

### 4.4.2 可重入锁与非可重入锁
根据同一个线程能否重复获取同一把锁，锁可以分为可重入锁与非可重入锁。

> 为什么需要可重入锁？

可重入锁主要用在线程需要多次进入临界区代码时，需要使用可重入锁。具体的例子，比如一个synchronized方法需要调用另一个synchronized方法时。可以避免死锁，也可以在同步方法被递归调用时使用。  

当一个线程要获取被其他线程持有的独占锁时，该线程会被阻塞，那么当一个线程可以再次获取**它自己已经持有的锁**，即不被阻塞则称为**可重入锁**，不可以再次获取，即获取时被阻塞，则称为**不可重入锁**，。一定要注意是同一个线程。Java 中典型的可重入锁是 ReentrantLock 和 synchronized。

synchronized 的可重入性质见 [Github示例](https://github.com/maoturing/concurrency_tools_practice/blob/master/src/main/java/lock/reentrantlock/SynchronizedLock.java)，下面是对于 ReentrantLock 可重入性质的演示，main线程多次获取已经持有的lock锁，getHoldCount() 表示：
```java
public class ReentrantLockDemo {
    public static ReentrantLock lock = new ReentrantLock();

    public static void main(String[] args) throws InterruptedException {
        ReentrantLockDemo demo = new ReentrantLockDemo();
        // 该线程可以多次获取可重入锁，并且不释放锁
        new Thread(() -> demo.testReetrant()).start();
        Thread.sleep(1000);
        // 该线程尝试获取可重入锁失败，因为锁被上一个线程持有
        new Thread(() -> demo.getLock()).start();
    }

    private void getLock() {
        lock.lock();
        System.out.println(Thread.currentThread().getName() + "获取到了锁");
    }

    /**
     * 多次获取可重入锁
     */
    private void testReetrant() {
        // 输出当前线程持有锁lock的次数
        System.out.println(lock.getHoldCount());
        // 当前线程对lock加锁
        lock.lock();

        System.out.println(lock.getHoldCount());
        lock.lock();
        System.out.println(lock.getHoldCount());
        lock.lock();
        System.out.println(lock.getHoldCount());
        lock.lock();
        System.out.println(lock.getHoldCount());
    }
}
```

线程获取可重入锁有三种情况： 1. 如果锁已经被其他线程持有，则进入阻塞等待状态； 2. 如果锁没有被其他线程持有，则获得锁，并将锁的当前持有者设置为当前线程 3. 如果锁被当前线程持有，则获得锁，并将锁的重入计数器+1，释放锁时会将计数器-1；

根据以上特点，可重入锁的简单实现如下：

```java
public class Lock{
    boolean isLocked = false;   // 表示当前锁是否被线程持有
    Thread  lockedBy = null;    // 表示当前锁被哪个线程持有
    int lockedCount = 0;
    public synchronized void lock()
            throws InterruptedException{
        Thread thread = Thread.currentThread();

        while(isLocked && lockedBy != thread){
            // 被锁住且锁的持有者不是当前线程，则进入阻塞等待状态
            this.wait();
        }
        isLocked = true;
        // 可重入锁，需要记录当前线程获取该锁的次数
        lockedCount++;
        // 标记该锁被当期线程持有
        lockedBy = thread;
    }
    public synchronized void unlock(){
        if(Thread.currentThread() == this.lockedBy){
            // 释放锁，重入计数器-1
            lockedCount--;
            if(lockedCount == 0){
                isLocked = false;
                // 释放锁，并唤醒其他等待获取该锁的线程
                this.notify();
            }
        }
    }
}
```

#### 4.4.3 公平锁与非公平锁

根据多个线程获取一把锁时是否先到先得，可以分为公平锁和不公平锁。

**公平锁**表示线程获取锁的顺序是按照线程请求锁的时间顺序决定的，也就是请求锁早的线程更早获取到锁，即先来先得。而**非公平锁**则在运行时插队，也就是先来不一定先得，但也不等于后来先得。

Java 中 ReentrantLock 提供了公平锁与非公平锁的实现：

* 公平锁： `ReentrantLock fairLock = new ReetrantLock(true);`
* 非公平锁：`ReentrantLock unFairLock = new ReetrantLock(false);` 不传递参数，默认非公平锁。

> 为什么需要非公平锁？

为了提高效率。假设线程 A 已经持有了锁，此时线程B请求该锁则会阻塞被挂起Suspend。当线程A释放该锁后，**此时恰好**有线程C也来请求此锁，如果采取公平方式，则线程B获得该锁；如果采取非公平方式，则线程B和C都有可能获取到锁。

Java中这样设计是为了提高效率，在上面的例子中，线程B被挂起，A释放锁后如果选择去唤醒B，则需要性能消耗和等待时间；如果直接给此时来请求锁（未被挂起）的线程C，则避免了唤醒操作的性能消耗，利用了这段空档时间，性能更好。总之，一定是能提升效率才会出现插队情况，否则一般不允许插队。

下图中 Thread1 持有锁时，等待锁的队列中有三个线程，当Thread1 释放锁时，恰好 Thread5 请求了锁，此时Thread5 就插队到最前面获取了锁。 ![&#x975E;&#x516C;&#x5E73;&#x9501;](https://raw.githubusercontent.com/maoturing/PictureBed/master/pic/20200325072211.png)

在现实中也有类似的例子，比如排队买早餐，摊主正在给A准备早餐，B则去旁边座位等待了，A的早餐刚好做完时，C来了，老板可能不会去花时间去叫并等待B，而会直接给C做，提高自己的效率。

在没有公平性需求的前提下尽量使用非公平锁，因为公平锁会带来性能开销。

公平锁与非公平锁的**验证**见 [Github示例](https://github.com/maoturing/concurrency_tools_practice/blob/master/src/main/java/lock/reentrantlock/FairLock.java)

#### 4.4.4 共享锁与排它锁

根据多个线程是否能够共享同一把锁，可以分为共享锁与排它锁。

**共享锁**，又称为读锁，可以同时被多个线程获取。获得共享锁后可以查看但无法修改和删除数据，其他线程也可以同时获得该共享锁，也只能查看不能修改和删除数据。ReentrantReadWriteLock 中的读锁就是共享锁，可以被多个线程同时获取

**排它锁**，又称为独占锁，不能被多个线程同时获取，平时最常见的都是排它锁，比如 synchronized，ReentrantLock都是排它锁。

> 为什么需要共享锁？

多个线程同时读数据，如果使用 ReentrantLock 则多个线程不能同时读，降低了程序执行效率。

如果在读的地方使用共享锁，写的地方使用排它锁。如果没有写锁的情况下，读是无阻塞的，提高了执行效率。

### 读写锁的规则

1. 多个线程可以一起读
2. 一个线程写的同时，其他线程不能读也不能写。
3. 一个线程读的同时，其他线程不能写

Java中读写锁的定义如下所示，完整的[代码示例见 Github](https://github.com/maoturing/concurrency_tools_practice/tree/9db5414f73f3fd73c68ebfd5a7950a4da30608aa/ReentrantReadWriteLockDemo/README.md)

```java
    private static ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
    private static ReentrantReadWriteLock.ReadLock readLock = reentrantReadWriteLock.readLock();
    private static ReentrantReadWriteLock.WriteLock writeLock = reentrantReadWriteLock.writeLock();

    private static void readText() {
        readLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + "得到读锁,正在读取...");
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println(Thread.currentThread().getName() + "释放读锁");
            readLock.unlock();
        }
    }

    private static void writeText() {
        writeLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + "得到写锁,正在写入...");
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println(Thread.currentThread().getName() + "释放写锁");
            writeLock.unlock();
        }
    }
```

读写锁 ReentrantReadWriteLock 也可以通过构造参数设置为公平锁和非公平锁，默认是非公平锁。**读锁插队能和其他读锁共享，可以提升效率，**~~**写锁是排它锁，不会出现插队情况**~~，所以下面讨论的均是读锁插队的情况：

**公平锁是不允许插队的，即先到先得**，不区分读写锁。但是读写锁 ReentrantReadWriteLock 作为非公平锁时插队策略有以下两种：

**1. 谁能获取谁优先策略**，如下图所示，当 Thread2\(R\) 在获得了读锁后，依次来了三个线程，Thread3\(W\) 请求写锁，Thread4\(R\) 和 Thread5\(R\) 请求读锁。

**谁能获取谁优先策略就是谁能获取谁优先**，Thread2\(R\) 持有读锁时，Thread3\(W\)虽然来的早但是无法获得写锁，Thread4\(R\) 和 Thread5\(R\) 来的晚但是可以获取读锁，所以优先出队。写锁因为是排它锁，所以不存在插队的情况。（注意这里与公平锁章节所说的恰好释放时请求锁提高效率情况不同）

![&#x8C01;&#x80FD;&#x83B7;&#x53D6;&#x8C01;&#x4F18;&#x5148;&#x7B56;&#x7565;](https://raw.githubusercontent.com/maoturing/PictureBed/master/pic/20200326183604.png)

但是该策略存在一个缺点，就是容易导致 Thread3\(W\) 出现饥饿问题，如果一直有其他线程来获取读锁，那么 Thread3\(W\) 可能永远请求不到写锁，导致饥饿问题。从用户角度出发，我先修改后读取，但是修改晚于读取生效也是不合理的。

**2. 避免饥饿策略**，如下图所示，当 Thread2\(R\) 在获得了读锁后，依次来了两个线程，Thread3\(W\) 请求写锁，Thread4\(R\) 请求读锁。

**避免饥饿策略就是等待队列头元素是请求写时，请求读不能插队**，Thread2\(R\) 持有读锁时，Thread3\(W\)来的早但是无法获得写锁，Thread4\(R\) 虽然可以获取读锁，但是来的比写锁 Thread3\(W\) 晚，所以也加入等待队列。直至 Thread3\(W\) 获取并释放了写锁，Thread4\(R\) 才可以获取读锁。

![&#x907F;&#x514D;&#x9965;&#x997F;&#x7B56;&#x7565;](https://raw.githubusercontent.com/maoturing/PictureBed/master/pic/20200326185413.png)

避免饥饿策略虽然牺牲了一些效率，但是解决了饥饿问题，并且更加符合人们的认知，所以这也是 JDK 中读写锁使用的插队策略。

Java 中 ReentrantReadWriteLock 读锁插队能提升效率，写锁是排它锁，不会出现插队情况，所以关于读锁插队策略总结如下：

* 公平锁：不允许插队，不区分读写锁，一律先到先得
* 非公平锁：读锁仅在可以等待队列**头部**不是请求写锁的线程时可以插队；如果等待队列头部是请求读锁，而当目前持有写锁的线程恰好释放写锁是，则新来的读线程会插队获得读锁，这点与非公平锁选择接受新线程而不去唤醒等待线程出策略一致。[请求读线程插队代码示例见 Github](https://github.com/maoturing/concurrency_tools_practice/tree/9db5414f73f3fd73c68ebfd5a7950a4da30608aa/NonfairBargeDemo/README.md)

```java
// ReentrantReadWriteLock内部类公平锁源码

// 公平锁请求读和请求写都需要去排队，除非队列中没有元素才去尝试获取锁
static final class FairSync extends Sync {
    private static final long serialVersionUID = -2274990926593161451L;
    // 判断写入线程是否应该阻塞Block，如果队列不为空，返回true表示应该阻塞去排队
    final boolean writerShouldBlock() {
        return hasQueuedPredecessors();
    }

    // 判断读取线程是否应该阻塞Block，如果队列不为空，返回true表示应该阻塞去排队
    final boolean readerShouldBlock() {
        return hasQueuedPredecessors();
    }
}
```

```java
// ReentrantReadWriteLock内部类非公平锁源码
static final class NonfairSync extends Sync {
    private static final long serialVersionUID = -8159625535654395037L;

    // 写线程不阻塞挂起，直接尝试插队
    final boolean writerShouldBlock() {
        return false;  
    }

    // 查看等待队列第一个元素是不是排他锁（写锁）
    // 如果是返回true，表示当前请求读线程应该阻塞挂起
    // 读锁不能插写锁的队，
    final boolean readerShouldBlock() {
        return apparentlyFirstQueuedIsExclusive();
    }
}
```

### ReentrantReadWriteLock 锁的升降级

> 为什么需要锁的升降级？

一个方法开始时需要写入，后面需要读取，为了减小锁的粒度，且方法执行过程中不希望被打断。如果支持写锁降级为读锁，就可以减小写锁的粒度，在读的部分，其他线程也可以一起来读取，并且方法执行不会被打断。（释放写锁重新获取读锁可能会阻塞等待）

ReentrantReadWriteLock 锁写锁可以降级为读锁提高执行效率，但不支持读锁升级为写锁，升级会将线程阻塞。ReentrantReadWriteLock 锁升级降级[示例代码见Github](jing-tong-java-bing-fa-xue-xi-bi-ji-zhi-suo.md)

> 面试题：为什么ReentrantReadWriteLock不支持锁的升级？

线程A和B目前都持有读锁，且都想升级为写锁，线程A会等待线程B释放读锁后进行升级，线程B也会等待线程A释放读锁后进行升级，这样就造成了死锁。当然，并不是说锁是无法升级的，比如可以限制每次都只有一个线程可以进行锁升级，这个需要具体的锁去进行实现。

#### 4.4.5 自旋锁与阻塞锁

根据等待锁的方式可以分为自旋锁和阻塞锁。

阻塞或唤醒一个 Java 线程需要操作同切换CPU状态来完成，这个状态

> 为什么需要自旋锁？
>
> **java的线程是与操作系统原生线程一一对应的，挂起和恢复线程都需要切换到内核态中完成。** 当一个线程获取锁失败后，会被切换到内核态挂起。当该线程获取到锁时，又需要将其切换到内核态来唤醒该线程，用户态切换到核心态会消耗大量的系统资源。

自旋锁则是线程获取锁时，如果发现锁已经被其他线程占有，它不会马上阻塞自己，而会进入忙循环（自旋）多次尝试获取（默认循环10次），避免了线程状态切换的开销。

> 面试题：自旋锁其实就是死循环，死循环会导致 CPU 一个核心的使用率达到100%，那为什么多个自旋锁并没有导致 CPU 使用率到达100%系统卡死呢？

默认自旋最多10次，可以使用`-XX:PreBlockSpin`（`-XX:PreInFlateSpin`）修改该值，在JDK1.6 中引入了**自适应的自旋锁**，自适应意味着自旋的时间不再固定了，而是由前一次在同一个锁上的自旋时间及锁的拥有者的状态来决定。

如果在同一个锁对象上，自旋等待刚刚成功获取过锁，并且持有锁的线程正在运行中，那么JVM会认为这次自旋也很有可能再次成功，进而将允许自旋等待更长时间，如果100次循环。

如果对于某个锁，自旋很少成功获得过，那么以后获取这个锁时将可能省略掉自旋过程，避免浪费处理器资源。 以上参考《深入理解JVM p298》

#### 4.4.6 可中断锁与不可中断锁

根据在等待锁的过程中是否可以中断分为可中断锁与不可中断锁。

在Java中，synchronized 就是不可中断锁，一旦线程开始请求synchronized锁，就会一直阻塞等待，直至获得锁。Lock 就是可中断锁，tryLock\(time\) 和 lockInterruptibly\(\) 都能在**请求锁的过程中响应中断，实现原理就是检测线程的中断标志位，如果收到中断信号则抛出异常**。

ReentrantLock\#tryLock\(time\) 使用AQS获取锁，每次AQS循环都会检测中断标志位，若标志位被修改，则抛出异常，中断获取锁，源码如下所示：

```java
    // ReentrantLock源码
    public boolean tryLock(long timeout, TimeUnit unit)
            throws InterruptedException {
        // 尝试获取锁
        return sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }


    public final boolean tryAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        // 判断中断标志位是否被修改，若被修改则抛出异常中断获取锁
        if (Thread.interrupted())
            throw new InterruptedException();
        // doAcquireNanos中使用AQS尝试获取锁，每次循环也都会检测中断标志位
        return tryAcquire(arg) || doAcquireNanos(arg, nanosTimeout);
    }
```

### 4.5 锁优化

JDK1.6 实现了各种锁优化技术，如自适应自旋（Adaptive Spinning）、锁消除（Lock Elimination）、锁粗化（Lock Coaresening）、轻量级锁（LightWeight Locking）和偏向锁（Biased Locking）等技术。这些技术都是为了线程之间更高效的共享数据，以及解决竞争问题。

#### 4.5.1 自适应自旋锁

互斥同步中对性能最大的影响是阻塞的实现，挂起线程和恢复线程的操作都需要操作系统切换到内核态来完成，这些操作对并发性能带来很大压力，

#### 4.5.2 锁消除

锁消除指的是在保证线程安全的前提下，JVM 删除一些不必要的锁。

锁消除是指虚拟机即时编译器在运行时，对一些代码上要求同步，但是被检测到不可能存在共享资源的竞争的锁进行消除。锁消除主要判定依据是来源于逃逸分析的数据支持，如果判断一段代码中，堆上的所有数据都不会逃逸出去而被其他线程访问到，那就可以把他们当做栈上数据来对待，认为是线程私有的，同步加锁自然无需进行。（逃逸分析见深入理解JVM）

比如下面拼接字符串的代码中，JDK5之前Javac编译器会将"+"拼接优化为StringBuffer，JDK5之后会优化为StringBuilder。

```java
    public String concatString0(String s1, String s2, String s3) {
        return s1 + s2 + s3;
    }

    public String concatString(String s1, String s2, String s3) {
        StringBuffer sb = new StringBuffer();
        sb.append(s1);
        sb.append(s2);
        sb.append(s3);
        return sb.toString();
    }
```

查看 StringBuffer\#append源码如下所示，使用了 synchronized 来保证拼接操作的线程安全。但是在上面的例子中，变量 sb 作用于被限制在 concatString\(\) 方法内部，即变量 sb 是线程私有的，不会出现线程安全问题（如果sb定义到属性中则会出现线程安全问题）。虽然这里有锁，经过JIT编译之后，这段代码会忽略掉 synchronized 锁来执行。

```java
    // StringBufferr#append源码
    @Override
    public synchronized StringBuffer append(String str) {
        toStringCache = null;
        super.append(str);
        return this;
    }
```

#### 4.5.3 锁粗化

锁消除指的是在保证线程安全的前提下，JVM 扩大锁的同步范围，来避免对同一对象的反复加锁解锁。

原则上，编写代码时推荐将同步块的范围尽量缩小，这样能够保证线程安全的同时让等待线程尽快拿到锁并发执行。但是如果当前线程一系列操作都是对同一个对象的反复加锁和解锁，甚至加锁解锁操作出现在循环体中，那即使没有线程竞争，频繁加锁解锁也会导致不必要的性能损耗。

比如下面的拼接字符串操作，每次执行 StringBuffer\#append 方法都会对同一个对象 sb 加锁，下面代码共执行了 3 次加锁解锁操作。如果 JVM 检测到这样一串操作都对**同一个对象反复加锁解锁**，**则会把加锁同步的范围扩展（粗化）到整个操作的外部**，就会将下面代码的加锁同步范围扩展到第一个 append\(\) 之前和最后一个 append\(\) 之后。

```java
    public String concatString(String s1, String s2, String s3) {
        StringBuffer sb = new StringBuffer();
        sb.append(s1);
        sb.append(s2);
        sb.append(s3);
        return sb.toString();
    }
```

#### 4.5.4 重量级锁

在并发编程中 synchronized 一直是元老级角色，很多人都会称呼synchronized 为重量级锁。在Java中每一个对象都可以作为锁，synchronized 实现同步分为以下 3 种情况： 1. 对于普通同步方法，锁是当前实例对象 2. 对于静态同步方法，锁是当前类的 Class 对象 3. 对于同步方法快，锁是 synchronized 括号中设置的对象

JVM 会将 synchronized 翻译成两条指令： `monitorenter` 和 `monitorexit`，分别表示获取锁与释放锁，这两个命令总是成对出现。

但是在JDK6中为了对 synchronized 进行优化，引入了**轻量级锁和偏向锁**，减少获取锁和释放锁带来的性能消耗。

#### 4.5.4 轻量级锁

轻量级锁是在**没有多线程竞争**的前提下，减少传统重量级锁使用操作系统互斥量Mutex产生的性能消耗。轻量级锁的加锁解锁是通过CAS完成的，避免了使用互斥量的开销，但是如果存在锁竞争，轻量级锁除了互斥量的操作还发生了CAS操作，性能反而更慢。

**Java中每一个对象都可以作为锁，对象头主要分为两部分，MarkWord 和 指向方法区对象类型数据的指针**，如果是数组对象，还会存储数组长度。MarkWord 中存储对象的哈希码，对象分代年龄，锁状态。

![&#x9501;&#x72B6;&#x6001;&#x4E0D;&#x540C;&#xFF0C;MarkWord&#x5185;&#x5BB9;&#x4E0D;&#x540C;](https://raw.githubusercontent.com/maoturing/PictureBed/master/pic/20200328043639.png)

### 锁升级

锁一共有4种状态，**锁升级过程依次是：无锁状态、偏向锁状态、轻量级锁状态和重量级锁状态**。这几种状态会随着竞争逐渐升级，锁可以升级但不能降级。

#### 4.5.5 偏向锁

偏向锁会偏向第一个获取它的线程，如果接下来执行过程中，该锁没有被其他线程获取，则持有偏向锁的线程永远不需要再同步。如果说轻量级锁是在无竞争情况下用CAS避免同步中使用互斥量，而偏向锁是等到竞争出现才释放锁，减少释放锁带来的性能消耗。

HotSpot 作者经过研究发现，大多数情况下，**锁不仅不存在多线程竞争，而且总是由同一线程多次获得**，为了让线程获得锁的代价更低引入了偏向锁。

当一个线程访问同步块并获取锁时，会在锁对象头存储锁偏向的线程ID，以后该线程进入和退出同步块都不需要进行CAS操作来加锁和解锁，只需要检查对象头中存储的线程ID是否为当前线程。

如果偏向线程ID等于当前线程，表示线程已经获取了锁。 如果偏向线程ID不等于当前线程，则尝试使用CAS将对象头的偏向锁指向当前线程。 如果不是偏向锁，则使用CAS竞争锁。

偏向锁是等到竞争出现才释放锁，减少释放锁带来的性能消耗，所以当其他线程尝试竞争偏向锁时，持有偏向锁的线程才会释放锁。

并没有完全搞懂，更多参考《Java并发编程的艺术 p13》和在《深入理解JVM p402》

### 4.6 ReentrantLock

学完AQS再来补充

### 4.7 ReentrantReadWriteLock

## 推荐阅读

1. [Java并发编程之美 - 翟陆续](https://book.douban.com/subject/30351286/) 内容和慕课网[玩转Java并发](https://coding.imooc.com/class/chapter/409.html)类似，可以配合阅读，有丰富的源码分析，实践部分有10个小案例
2. [Java并发编程实战 - 极客时间](https://time.geekbang.org/column/intro/159) 内容有深度，并发设计模式，分析了 4 个并发应用案例 Guava RateLimiter，Netty，Disrupter 和 HiKariCP，还介绍了 4 种其他类型的并发模型 Actor，协程，CSP等
3. [精通Java并发编程 - 哈维尔](https://book.douban.com/subject/30327401/)  非常多的案例，几乎每个知识点和章节都有案例，学习后能更熟悉Java并发的应用
4. [死磕Java并发源码系列 - 大明哥](http://cmsblogs.com/?cat=151)
5. [精尽Java并发源码系列 - 芋道源码](http://www.iocoder.cn/JUC/good-collection/)
6. 传智播客8天并发  笔记有并发案例，CPU原理等笔记，非常深入，后面画时间学习一下精

## 参考文档

1. [玩转Java并发工具，精通JUC - 慕课网](https://coding.imooc.com/class/chapter/409.html)
2. [Java并发在线思维导图 - 慕课网](http://naotu.baidu.com/file/ab389987308c34fdc57beb911cd0eb80?token=39caec33969b1e00)
3. [Java多线程编程实战 - 汪文君](https://www.bilibili.com/video/av43529474)
4. [Java并发编程实战 - 极客时间](https://time.geekbang.org/column/intro/159)
5. [Java并发编程之美 - 翟陆续](https://book.douban.com/subject/30351286/)
6. 实战Java高并发程序设计 - 葛一鸣
7. [四天学懂 JUC - 周阳](https://www.bilibili.com/video/av70166821)  
8. [小白科普：线程和线程池 - 码农翻身](https://mp.weixin.qq.com/s/qzoLgNNSZD2NrzBEINVuUg)
9. [利用常见场景详解java线程池 - CarpenterLee](https://mp.weixin.qq.com/s/N-2Uv8UewqweGXXAJ63jyQ)
10. [Java线程池总结 - 后端技术精选](https://mp.weixin.qq.com/s/inJCn05ysxcOzCsCsFeDag)
11. [快速上手SpringBoot:线程池的集成使用](https://mp.weixin.qq.com/s/z3gjfk4l-s8aKD4cvY8CHA)

