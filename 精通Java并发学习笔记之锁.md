<!-- TOC -->

- [4 千变万化的锁](#4-千变万化的锁)
    - [4.1 Lock 接口](#41-lock-接口)
    - [4.2 Lock 常用 5 个方法](#42-lock-常用-5-个方法)
    - [4.3 Lock 的可见性](#43-lock-的可见性)
    - [4.4 锁的分类](#44-锁的分类)
        - [4.4.1 乐观锁和悲观锁](#441-乐观锁和悲观锁)
        - [4.4.2 可重入锁与非可重入锁](#442-可重入锁与非可重入锁)
        - [4.4.3 公平锁与非公平锁](#443-公平锁与非公平锁)
- [推荐阅读](#推荐阅读)
- [参考文档](#参考文档)

<!-- /TOC -->

# 4 千变万化的锁

## 4.1 Lock 接口
锁是一种工具，用于控制对**共享资源**的访问，我们已经有了 synchronized 锁，为什么还需要 Lock 锁呢？

synchronized 锁存在以下问题：
1. 效率低：试图获取锁时不能设定超时，会一直等待
2. 不够灵活：加锁释放锁时机单一
3. 无法知道是否已经获取了锁

## 4.2 Lock 常用 5 个方法
在Lock 中声明了 4 个方法来获取锁：
- `lock()`  获取锁，如果锁被其他线程获取，则进行等待。Lock 不会像synchronized 自动释放锁，即使发生异常，也能争取释放锁，Lock 需要在 finally 中释放锁，以保证在发生异常时锁被正确释放。`lock()`方法不能被中断，获取不到锁则会一直等待，一旦陷入死锁`lock()`会进入永久等待。
- `unlock()` 释放锁，如果当前线程没有持有该锁调用该方法会抛出 IllegalMonitorStateException 异常
```java
public class LockDemo {

    public static final Lock lock = new ReentrantLock();

    public static void main(String[] args) throws InterruptedException {
        LockDemo lockDemo = new LockDemo();
        // 只有第一个线程能获取锁，另一个线程会一直等待
//        new Thread(() -> lockDemo.testLock()).start();
//        new Thread(() -> lockDemo.testLock()).start();


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
}
```
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

乐观锁认为修改数据在一般情况下不会造成冲突，所以在修改记录前不会加锁，但在数据提交更新时，才会对数据冲突与否进行检测，检查我修改数据期间，有没有其他线程修改过，一般通过 CAS 算法实现。Java中的典型乐观锁是原子类和并发容器。

在数据库中就有对乐观锁的典型应用：要更新一条数据，首先查询数据的version:`select * from table;`
然后更新语句，update set num=2，version=version+1 where version=1 and id=5,如果数据没有被其他人修改，则version与查询数据时的version一直都为1，则可以修改成功，并返回version=2，如果被其他人修改了，则重新查询和更新数据。

**开销对比：**
1. 悲观锁的原始开销要高于乐观锁，但是特点是一劳永逸。适合资源竞争激烈的情况，持有锁时间长的情况
2. 乐观锁如果自旋时间很长或不停重试，消耗的资源也会越来越多。适用于资源竞争不激烈的情况

**适用场景：**

悲观锁，适合并发写入多，持有锁时间较长的情况，如临界区有IO操作，临界区代码复杂循环量大，临界区竞争激烈等情况
乐观锁，适合并发写入少，不加锁能提高效率。如少写多读数据的场景，数据改变概率小，自旋次数少。

### 4.4.2 可重入锁与非可重入锁
根据线程能否重复获取同一把锁，锁可以分为乐观锁与悲观锁。

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

### 4.4.3 公平锁与非公平锁

根据线程获取锁的抢占机制可以分为公平锁和不公平锁。

公平锁表示线程获取锁的顺序是按照线程请求锁的时间顺序决定的，也就是请求锁早的线程更早获取到锁，即先来先得。而非公平锁则在运行时闯入，也就是先来不一定先得，但也不等于后来先得。

Java 中 ReentrantLock 提供了公平锁与非公平锁的实现：
- 公平锁： ReentrantLock fairLock = new ReetrantLock(true);
- 非公平锁： ReentrantLock unFairLock = new ReetrantLock(false); 不传递参数，默认非公平锁。

假设线程 A 已经持有了锁，此时线程B请求该锁则会阻塞被挂起Suspend。当线程A释放该锁后，**此时恰好**有线程C也来请求此锁，如果采取公平方式，则线程B获得该锁；如果采取非公平方式，则线程B和C都有可能获取到锁。

Java中这样设计是为了提高效率，在上面的例子中，线程B被挂起，A释放锁后如果选择去唤醒B，则需要性能消耗和等待时间；如果直接给此时来请求锁（未被挂起）的线程C，则避免了唤醒操作的性能消耗，利用了这段空档时间，性能更好。在现实中也有类似的例子，比如排队买早餐，摊主正在给A准备早餐，B则去旁边座位等待了，A的早餐刚好做完时，C来了，老板可能不会去花时间去叫并等待B，而会直接给C做，提高自己的效率。

在没有公平性需求的前提下尽量使用非公平锁，因为公平锁会带来性能开销。


ReentrantLock详解
AQS 
isHeldByCurrentThread
getHoldCount


1. 锁的分类
2. 乐观锁和悲观锁
3. 可重入锁和非可重入锁
4. 公平锁和非公平锁
5. 共享锁和排它锁
6. 自旋锁和阻塞锁
7. 可中断锁
8.  锁优化
9. 





# 推荐阅读
1. [Java并发编程之美 - 翟陆续](https://book.douban.com/subject/30351286/)  内容和慕课网[玩转Java并发](https://coding.imooc.com/class/chapter/409.html)类似，可以配合阅读，有丰富的源码分析，实践部分有10个小案例
   
2. [Java并发编程实战 - 极客时间](https://time.geekbang.org/column/intro/159)  内容有深度，并发设计模式，分析了 4 个并发应用案例 Guava RateLimiter，Netty，Disrupter 和 HiKariCP，还介绍了 4 种其他类型的并发模型 Actor，协程，CSP等
3. [精通Java并发编程 - 哈维尔](https://book.douban.com/subject/30327401/)  非常多的案例，几乎每个知识点和章节都有案例，学习后能更熟悉Java并发的应用
4. 
5. 传智播客8天并发  笔记有并发案例，CPU原理等笔记，非常深入，后面画时间学习一下精

# 参考文档
1. [玩转Java并发工具，精通JUC - 慕课网](https://coding.imooc.com/class/chapter/409.html)
   
2. [Java并发在线思维导图 - 慕课网](http://naotu.baidu.com/file/ab389987308c34fdc57beb911cd0eb80?token=39caec33969b1e00)
3. [Java多线程编程实战 - 汪文君](https://www.bilibili.com/video/av43529474)
4. [Java并发编程实战 - 极客时间](https://time.geekbang.org/column/intro/159)
5. [Java并发编程之美 - 翟陆续](https://book.douban.com/subject/30351286/)
6. 实战Java高并发程序设计 - 葛一鸣
7. [四天学懂 JUC - 周阳](https://www.bilibili.com/video/av70166821)  
8. [小白科普：线程和线程池 - 码农翻身](https://mp.weixin.qq.com/s/qzoLgNNSZD2NrzBEINVuUg)
9.  [利用常见场景详解java线程池 - CarpenterLee](https://mp.weixin.qq.com/s/N-2Uv8UewqweGXXAJ63jyQ)
10. [Java线程池总结 - 后端技术精选](https://mp.weixin.qq.com/s/inJCn05ysxcOzCsCsFeDag)
11. [快速上手SpringBoot:线程池的集成使用](https://mp.weixin.qq.com/s/z3gjfk4l-s8aKD4cvY8CHA)
