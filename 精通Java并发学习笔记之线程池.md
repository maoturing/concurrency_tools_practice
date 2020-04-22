
# 0. 前言
> 为什么需要学习并发编程？

Tomcat、Netty等框架源码，需要并发编程基础才能看懂；
并发也是Java程序员的必经之路

本篇文章的学习内容有：
1. 20+并发工具：线程池，各种锁，原子类，并发容器
2. 两种并发策略：ThreadLocal和final
3. 两大底层原理：CAS原理与AQS框架
4. 控制并发流程：Semaphore
5. 实战高性能缓存

# 1. 总览并发工具

并发工具JUC（Java Util Concurrent）类根据功能可分为三大类：
1. 并发安全：
   1. 从底层原理分类：互斥同步（锁）、非互斥同步（atomic）、无同步方案（final）
   2. 从使用角度分类：限制共享变量，避免共享变量，成熟并发工具
2. 管理线程：线程池
3. 线程协作：三大并发工具类等

更加详细的分类参考[思维导图](http://naotu.baidu.com/file/ab389987308c34fdc57beb911cd0eb80?token=39caec33969b1e00)的建立并发知识框架分支


根据JUC类可以分为以下5类：
Executors: 线程池
Atomic: 原子类
Lock：锁
Tools：并发工具
Collections: 并发集合

![JUC 核心类图](https://raw.githubusercontent.com/maoturing/PictureBed/master/pic/20200422224518.png)


# 2. 线程池-线程治理最大法宝

线程池是并发工具中用来**管理线程**的工具。

码农翻身的这篇[小白入门线程池](https://mp.weixin.qq.com/s/qzoLgNNSZD2NrzBEINVuUg)文章对线程池有一个大致的概念和介绍，推荐阅读。

## 2.1 什么是线程池？

> 为什么要使用线程池？

使用 new Thread 的方式创建线程执行任务，存在两个问题：
1. **反复创建线程开销大**，线程创建需要开辟虚拟机栈、本地方法栈、程序计数器等线程私有的内存空间，线程销毁时需要回收这些资源，频繁创建销毁线程会浪费大量系统资源
2. **过多的线程会占用大量CPU和内存**，大量线程回收会带来GC压力，甚至会出现 OOM 异常，CPU频繁的进行上下文切换也会降低系统性能


线程池主要解决两个问题：
1. **提升响应速度**。不需要反复创建和回收线程，消除了创建线程的延迟和销毁线程的时间
2. **合理利用CPU和内存资源，提供更好的性能**。灵活调整线程数量，不会线程太多导致OOM，也不会线程太少浪费CPU资源。如果不使用线程池，每次需要执行异步任务时直接 new 一个线程来运行，而线程的创建和销毁都是需要开销的。线程池里面的线程是可复用的，不需要每次执行任务时都重新创建和销毁线程，节省了计算机资源  
3. **统一管理线程**，比如停止线程池中的3000个线程，比挨个停止线程方便很多；可以动态新增线程，限制线程个数。每个线程池也都保留了一些基本的统计数据，比如当前线程池完成的任务数目等。

**线程池的适用场景**：
1. 服务器接收到大量请求时，适用线程池可以大大减少线程的创建和销毁，提高服务器的性能，实际中Tomcat也是这么做的
2. 实际开发中，如果需要创建5个以上的线程，那么就可以使用线程池来管理。


> 面试题1：线程不能重复 start，为什么线程池线程可以复用？
> 
线程重复 start，会报 IllegalThreadStateException 异常，因为线程声明周期就是从 start 到 terminated，没有办法从 terminated 恢复到 start，详细答案参考面试题4与2.8 runWorker源码解析章节

## 2.2 线程池详解与 6 大属性
JDK 中 ThreadPoolExecutor 类的构造方法如下：
```java
public ThreadPoolExecutor(int corePoolSize,
                            int maximumPoolSize,
                            long keepAliveTime,
                            TimeUnit unit,
                            BlockingQueue<Runnable> workQueue,
                            ThreadFactory threadFactory,
                            RejectedExecutionHandler handler) {
```
线程池创建主要有**6大参数**
| 参数名        | 类型                     | 含义                                              |
| :------------ | ------------------------ | ------------------------------------------------- |
| corePoolSize  | int                      | 核心线程数数                                        |
| maxPoolSize   | int                      | 最大线程数数                                        |
| keepAliveTime | long                     | 线程保持存活时间                                  |
| workQueue     | BlockingQueue            | 任务存储队列                                      |
| threadFactory | ThreadFactory            | 线程工厂，线程池需要新线程时使用ThreadFactory创建 |
| handler       | RejectedExecutionHandler | 线程池无法接受提交的任务时的拒绝策略              |

- `corePoolSize` 常驻核心线程数，如果等于0，则任务执行完毕，会销毁线程池所有线程；如果大于0，任务执行完毕核心线程不会销毁。这个值的设置非常关键，过大会浪费资源，过小灰导致线程被频繁创建和销毁。
- `maximumPoolSize` 最大线程数，表示线程池能够容纳同时执行的最大线程数，必须大于等于1。如果待执行的线程数大于此值，则缓存在任务队列 workQueue 中。如果 maxPoolSize 等于 corePoolSize，即是固定大小线程池

- `workQueue` 任务队列，当线程数大于等于 corePoolSize，则新任务保存到 BlockingQueue 阻塞队列。阻塞队列是线程安全的，使用两个 ReentrantLock 锁来保证从出队和入队的原子性，是一个生产消费模型队列

### 2.2.1 线程池添加线程规则
---


任务提交到线程池，添加线程规则如下：

可记为：**一cool二queue三max最后 reject**。
![任务处理规则](https://raw.githubusercontent.com/maoturing/PictureBed/master/pic/微信图片_20200317154944.jpg)

1. 如果线程数小于 corePoolSize，即使其他线程处于空闲状态，也会**创建一个新线程来执行新任务**。
2. 如果线程数等于（或大于）corePoolSize 但小于 maxPoolSize，则**将任务放入队列**
3. 如果队列已满，且线程数小于 maxPoolSize，则**创建一个新线程来执行任务**
4. 如果队列已满，且线程数等于 maxPoolSize，则**使用拒绝策略拒绝该任务**

**比喻：** 线程池中的线程就像**烧烤店的餐桌**，店里面的餐桌数 corePoolSize，坐满后让顾客在排队区 workQueue 排队等待；如果顾客少店内坐不满，店内餐桌也不会收起来；

如果排队区也满了，那只能冒着被城管罚款的风险在店外面广场加桌子，广场面积有限，最大餐桌数为 maxPoolSize；如果广场的顾客吃完了，那抓紧把桌子收起来；

如果排队区和广场都坐满了，那来了新顾客只能拒绝服务 rejected 了。 

![添加线程规则](https://raw.githubusercontent.com/maoturing/PictureBed/master/pic/20200307235727.png)

**源码分析：** 这里参考了《码出高效p243》，execute方法的作用是提交任务`command`到线程池执行，用户提交任务到线程池的模型图如下所示

![任务提交到线程池](https://raw.githubusercontent.com/maoturing/PictureBed/master/pic/20200315005328.png)
![任务执行方法流程](https://raw.githubusercontent.com/maoturing/PictureBed/master/pic/微信图片_20200317154949.jpg)

ThreadPoolExecutor采取上述步骤的总体设计思路，是为了在执行execute()方法时，尽可能地避免获取全局锁（那将会是一个严重的可伸缩瓶颈）。

在ThreadPoolExecutor完成预热之后（当前运行的线程数大于等于corePoolSize），几乎所有的execute()方法调用都是执行步骤2，而步骤2不需要获取全局锁。

```java
/*
 * ThreadPoolExecutor 源码
 * ctl.get()获取表示线程池状态和线程个数的int值，
 * workerCountOf获取线程池中的线程数，isRunning判断线程池是否为Running，是Running才接受新任务
 */
public void execute(Runnable command) {
    if (command == null)
        throw new NullPointerException();
    /*
        * Proceed in 3 steps:
        *
        * 1. If fewer than corePoolSize threads are running, try to
        * start a new thread with the given command as its first
        * task.  The call to addWorker atomically checks runState and
        * workerCount, and so prevents false alarms that would add
        * threads when it shouldn't, by returning false.
        *
        * 2. If a task can be successfully queued, then we still need
        * to double-check whether we should have added a thread
        * (because existing ones died since last checking) or that
        * the pool shut down since entry into this method. So we
        * recheck state and if necessary roll back the enqueuing if
        * stopped, or start a new thread if there are none.
        *
        * 3. If we cannot queue task, then we try to add a new
        * thread.  If it fails, we know we are shut down or saturated
        * and so reject the task.
        */
    // 返回包含线程池状态和线程数的 int 值，这个值的详解见线程池状态章节
    int c = ctl.get();
    // 1. 线程数小于常驻核心线程数，使用addWorker创建新线程，传入任务command
    if (workerCountOf(c) < corePoolSize) {
        // 创建线程成功则return返回
        if (addWorker(command, true))
            return;
        // 如果创建失败，防止外部已经在线程池中加入新任务，重新获取c
        c = ctl.get();
    }
    
    // 2. 如果线程数大于等于coresize，且线程池处于Running状态，则将任务添加到队列
    // 如果线程池调用了shutdown方法，则isRunning返回false，不会将任务添加到队列
    if (isRunning(c) && workQueue.offer(command)) {
        int recheck = ctl.get();
        
        // 2.2 添加成功后返回true，再次检查是否需要添加一个线程(因为入队过程中可能有线程被销毁core=0 keeptime=0)，
        // 因为非Running状态都不接受新任务，如果isRunning返回false则从队列移除任务command，并执行拒绝策略
        if (! isRunning(recheck) && remove(command))
            reject(command);
        // 2.3 如果当前线程池为空，则新创建一个线程
        else if (workerCountOf(recheck) == 0)
            addWorker(null, false);
    }
    // 3. 如果线程数大于等coresize，且任务队列已满，则创建新线程，
    // 参数false表示线程数要小于maxsize，创建成功返回true，失败则执行拒绝策略
    else if (!addWorker(command, false))
        // 如果addWorker返回false，即创建线程失败，则使用拒绝策略
        reject(command);
}
```
代码1判断如果线程池中线程个数小于 corePoolSize，则会向workers 里新增一个核心core线程执行该任务。

如果线程池中线程个数大于等于 corePoolSize 则执行代码2，如果当前线程池是RUNNING状态则添加任务到任务队列，非RUNNING状态是拒绝接受新任务的。

如果向任务队列添加成功，则代码2.2对线程池状态进行二次校验，这是因为添加任务到队列后，执行代码2.2前有可能线程池的状态已经有变化了。这里二次校验如果线程池状态不是RUNNING，则把该任务从任务队列移除，移除后政治性拒绝策略；如果二次校验通过，则执行代码2.3判断线程池中线程个数，如果线程个数为0则新建一个线程。

如果代码2添加任务失败，则说明任务队列已满，那么执行代码3，尝试新创建线程来执行该任务，如果**线程池线程个数 ≥ maxxinumPoolSize** 则执行拒绝策略。

**执行拒绝策略的两种情况**：1. 线程池状态为非RUNNING；2. 任务队列已满且线程个数 ≥ maxxinumPoolSize


### 2.2.2 常见 3 种工作队列
---
1. **空队列：SynchronousQueue**，队列长度为0，仅起到一个交接作用。任务保存到队列后直接交给线程池，很容易创建新的线程，所以线程池需要设置大一点的 maxPoolSize。比如`Executors#newCachedThreadPool`创建线程池就使用的该队列，并且设置 maxPoolSize 为 Integer.MAX_VALUE。
2. **无界队列：LinkedBlockingQueue**，链表结构，队列长度默认为 Integer.MAX_VALUE。由于队列不会满，所以线程数不会大于 corePoolSize，所以 maxPoolSize 相当于没有意义。可以应对流量突增，新任务都会被放到队列中，缺点是如果处理速度小于任务提交速度，则会造成 **OOM 异常**。比如`Executors#newFixedThreadPool`创建线程池使用的就是该队列，并且 maxPoolSize 等于 corePoolSize。
3. **有界队列：ArrayBlockingQueue**，需要手动指定长度的有界队列，使用该队列 maxPoolSize 有意义，当队列满了之后，会创建新线程来执行任务。

### 2.2.3 线程池 OOM
----
前面提到无界队列 LinkedBlockingQueue，如果处理速度小于任务提交速度则会出现 OOM，JDK 中`Executors#newFixedThreadPool`创建的线程池就使用的 LinkedBlockingQueue，使用不当就会出现OOM，[示例代码](https://github.com/maoturing/concurrency_tools_practice/blob/master/src/main/java/threadpool/FixedThreadPoolOOM.java)中任务的提交速度远高于执行速度，并且使用JVM 参数调小堆内存`-Xmx8m -Xms8m`，方便复现OOM。


> 面试题2：线程池线程数能大于maxPoolSize吗？

// 待解答，看源码貌似可以，但是与定义不符啊

> 面试题3：线程池没有任务时会怎么样？

会阻塞住，线程池没有任务执行时，会从任务队列 workQueue 取出任务，如果 workQueue 为空，则会调用 wait() 方法进入阻塞状态，直到有新任务进来唤醒。实际中大多使用LinkedBlockingQueue 阻塞队列，这里附上了简单的 ArrayBlockingQueue源码

```java
// ArrayBlockQueue#take源码
public E take() throws InterruptedException {
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    try {
        while (count == 0)
            // 如果阻塞队列位空，则进入阻塞状态，等待offer方法唤醒
            notEmpty.await();
        return dequeue();
    } finally {
        lock.unlock();
    }
}
```
查看 2.8.3 getTask源码分析章节，当没有任务时，核心线程会从任务队列 take 任务进入阻塞状态，非核心线程会从任务队列 poll 任务，若超时后没有任务则线程被回收（执行结束）。


- `keepAliveTime` 线程空闲时间，单位为 TimeUnit。当线程空闲时间达到 keepAliveTime 时为空闲线程，**空闲线程会被回收销毁，直到只剩下 corePoolSize 个线程为止**，避免浪费内存和句柄资源？。默认情况下，**线程数大于 corePoolSize时，keepAliveTime 才会起作用**。比如`Executors#newCachedThreadPool`就设置 keepAliveTime 为60秒，当超过线程空闲时间超过60s时，则会回收线程直到只剩下 corePoolSize 个线程为止。
- `threadFactory` 线程工厂。用来创建线程，默认使用 Executors.defaultThreadFactory()，创建出来的线程都属于同一个线程组，相同优先级NORM_PRIORITY，都不是守护线程，线程名称形如 pool-poolNumber-trhead-id，自定义线程工厂可以设置更加友好易读的线程名称。
```java
// Executors 内部类 DefaultThreadFactory源码，参数r是用户传入的任务
public Thread newThread(Runnable r) {
    Thread t = new Thread(group, r,
                            namePrefix + threadNumber.getAndIncrement(),
                            0);
    // 设置为非守护线程
    if (t.isDaemon())
        t.setDaemon(false);
    // 设置相同优先级
    if (t.getPriority() != Thread.NORM_PRIORITY)
        t.setPriority(Thread.NORM_PRIORITY);
    return t;
}
```
上述的Executors 默认的线程工厂过于简单，对用户不够友好，线程名称必须具有特定意义，如包含调用来源，业务含义等。清晰的线程名称方便后期调试和分析，有助于快速定位死锁，StackOverflow等问题。以下为简单的自定义线程工厂：
[去Github查看代码示例](https://github.com/maoturing/concurrency_tools_practice/blob/master/src/main/java/threadpool/demo/UserThreadFactory.java)

### 2.2.4 常见 4 种拒绝策略
----
- `handler` 线程池该属性用于执行拒绝策略，默认使用 RejectedExecutionHandler。当任务队列 workQueue 已满且线程数已经达到了 maxPoolSize，则通过该策略拒绝处理请求，这是一种简单的限流保护。友好的拒绝策略应该做到以下三点：
  
    1. 保存任务到数据库进行削峰填谷，在空闲时再取出来执行
    2. 转向某个提示页面
    3. 打印日志

在`ThreadPoolExecutor`中提供了 4 个公开的静态内部类：
- AbortPolicy（默认）：丢弃任务并抛出`RejectedExecutionException`异常
- DiscardPolicy：丢弃任务，但是不抛出异常，不推荐
- DiscardOldestPolicy：丢弃队列中等待最久的任务，然后把当前任务加入队列中
- CallerRunsPolicy：调用任务的`run()`方法绕过线程池直接执行。

一般使用默认的拒绝策略即可，如果需要自定义拒绝策略，可以参考[代码示例](https://github.com/maoturing/concurrency_tools_practice/blob/master/src/main/java/threadpool/demo/UserRejectHandler.java)

## 2.3 常见5种线程池

![线程池相关类图](https://raw.githubusercontent.com/maoturing/PictureBed/master/pic/20200308201254.png)

Executor 接口定义线程池执行任务的方法 execute()，

Executors 提供创建线程池的各种方式，类似于Collections

ExecutorService 接口定义了管理线程任务的方法 submit()、shutdown() 和 invokeAll()

AbstractExecutorService 对 submit()和 invokeAll() 进行实现。



| Parameter      | FixedThreadPool | SingleTExecutor | CachedTPool  | ScheduledTPool |
| -------------- | --------------- | -------------------- | ----------------- | ------------------- |
| corePoolSize   | n               | 1                    | 0                 | n                   |
| maxNumPoolSize | n               | 1                    | Integer.MAX_VALUE | Integer.MAX_VALUE   |
| keepAliveTime  | 0 s             | 0 s                  | 60 s              | 60 s                |
| workQueue      |LinkedBlockingQueue|LinkedBlockingQueue |SynchronousQueue   |DelayedWorkQueue     |

以下方法都有多种重载方法，可以设置线程数和自定义线程工厂，前三个返回ThreadPoolExecutor 对象，第四个返回 ScheduledThreadPoolExecutor 对象。
- `Executors#newSingleThreadExecutor()` 创建一个单线程的线程池，相当于单线程串行执行所有任务。
  ![20200317163707.png](https://raw.githubusercontent.com/maoturing/PictureBed/master/pic/20200317163707.png)
- `Executors#newFixedThreadPool(n)`  创建指定固定线程数的线程池，core 等于 maxSize，不存在空闲线程，所以keepAliveTime为 0
  
- `Executors#newCachedThreadPool()` 高度可伸缩线程池，coreSize为0，任务队列长度为0，任务直接交给线程池创建线程执行，线程空闲时间超过60s会被回收。
-  `Executors#newScheduledThreadPool()`  适支持定时及周期性任务执行，比 Timer 更安全，功能更强大。是`ScheduledThreadPoolExecutor`的对象。

// 补充 各种线程池执行任务的方法调用图，见java并发编程的艺术p214

通过了解以上4种自动创建线程池的方法，每种方法都有不恰当之处，maxNumPoolSize 设置过大，workQueue 设置为无界队列，在流量突增时都会引发 OOM，所以《阿里巴巴Java编程规范》中**不允许使用 Executors，推荐使用 ThreadPoolExecutor 的方式创建线程池**，这样的处理方式能更加明确线程池的运行规则，规避资源耗尽的风险。

- `Executors#newWorkStealingPool()` JDK8引入，创建持有足够线程的线程池。从下面源码中可以看出，该线程池把 CPU 核心数量设置为默认的并行度。通过线程池类图可知，其他线程池本质都是`ThreadPoolExecutor`，而`Executors#newWorkStealingPool()`创建的线程池是`ForkJoinPool`对象

```java
// Executors 创建线程池源码
public static ExecutorService newWorkStealingPool() {
    // 线程数为 CPU 核心数
    return new ForkJoinPool
        (Runtime.getRuntime().availableProcessors(),
            ForkJoinPool.defaultForkJoinWorkerThreadFactory,
            null, true);
}
```
这个线程池和前面的线程池有所不同，在能产生**子任务**的场景才适合使用该线程池。线程可以**窃取**（Stealing）其他线程的任务，提高并行度。

需要注意的是最好不要加锁，因为任务并行执行，不加锁才能发挥出并行的效果。不保证执行顺序，使用场景有限。例如递归情况，可以使用该线程池。



### 2.3.1 CachedThreadPool
![20200317163115.png](https://raw.githubusercontent.com/maoturing/PictureBed/master/pic/20200317163115.png)

// **补充** 这两个章节按照Java并发编程的艺术p214，和Java并发编程之美补充

### 2.3.2 ScheduledThreadPool 详解

![20200317163235.png](https://raw.githubusercontent.com/maoturing/PictureBed/master/pic/20200317163235.png)




## 2.4 手动创建线程池

上文中说道，手动创建线程池优于JDK自动创建线程池，但是线程池中的线程数应该设置为多少呢？

参考文章：[创建多少线程才是合适的？](https://time.geekbang.org/column/article/86666)

- **CPU 密集型**：比如加密、计算 hash 等任务，**任务属于 CPU 密集型，最佳线程数为 CPU 核心数**。
 
- **耗时 IO 型**：比如读写数据库、文件和网络通信等，CPU一般是不工作的，外设的速度远远慢于 CPU，最佳线程数可以设置为 cpu 核心数的很多倍。
- 线程数 = cpu 核心数 * （1+平均等待时间/平均工作时间）
- 动态获取 CPU 核心数`int n = Runtime.getRuntime().availableProcessors()`

**手动创建线程池**，应该注意以下 4 点：

1. 避免误解队列，防止 OOM 
2. 自定义线程工厂，设置合适的线程名称，方便后期调试
3. 一般使用默认的拒绝策略，也可以自定义拒绝策略
4. 设置合适的 corePoolSize 和 maxPoolSize

代码示例如下所示，[去Github查看详细代码示例](https://github.com/maoturing/concurrency_tools_practice/blob/master/src/main/java/threadpool/demo/UserThreadPool.java)
```java
public class UserThreadPool {
    /**
     * 执行main方法会打印如下日志：
     *  UserThreadFactory's 第1机房-Worker-1       // 线程工程创建线程，打印线程名称
     *  UserThreadFactory's 第1机房-Worker-2
     *  running_0                                 // task内容，打印任务执行次数
     *  running_1
     *  running_2
     *  running_3
     *  you task is rejected. count=158. java.util.concurrent.ThreadPoolExecutor@677327b6[Running, pool size = 2, active threads = 2, queued tasks = 2, completed tasks = 4]
     *      // 打印线程池状态，线程池线程数量为2，达到了我们定义的maxPoolSize=2，任务队列数量为2，达到了我们定义的workQueue容量，完成的任务数completed tasks有4个。
     *      // 到最终日志，任务的执行次数 running_和任务拒绝次数rejectCount相加等于总任务数200
     *      // 有时 queued tasks 不一定等于2，因为执行拒绝策略时队列元素为2，打印时队列元素可能已经被取走执行了，复现时可以删除 threadpool.demo.Task 类的 sleep方法。
     *
     */
    public static void main(String[] args) {
        // 1. 任务队列，避免误无解队列，防止OOM异常
        BlockingQueue workQueue = new LinkedBlockingQueue(2);

        // 2. 线程工厂，定义合适的线程名称
        UserThreadFactory f1 = new UserThreadFactory("第1机房");

        // 3. 拒绝策略
        UserRejectHandler handler = new UserRejectHandler();
        
        // 4.1 创建线程池，使用自定义线程工厂和拒绝策略
        ThreadPoolExecutor threadPool1 = new ThreadPoolExecutor(1, 2, 60, TimeUnit.SECONDS, workQueue, f1, handler);
        // 4.2 使用默认的线程工厂和拒绝策略，使用大多数情况，这里的拒绝策略时AbortPolicy，即丢弃任务并抛出异常
        ThreadPoolExecutor threadPool2 = new ThreadPoolExecutor(1, 2, 60, TimeUnit.SECONDS, workQueue);

        // 使用线程池，
        Runnable task= new Task();
        for (int i = 0; i < 200; i++) {
            // 使用自定义默认策略，拒绝任务并打印线程池状态
            // 任务的执行次数 running_和任务拒绝次数rejectCount之和应为总任务数200
            threadPool1.execute(task);

            //threadPool2.execute(task);    // 使用默认的拒绝策略，拒绝任务，并抛出异常
        }
    }
}
```

guava 创建线程池最佳实践
---

首先需要引入 guava 依赖:
```xml
<dependency>
  <groupId>com.google.guava</groupId>
  <artifactId>guava</artifactId>
  <version>28.2-jre</version>
</dependency>
```
接下来就可以使用 guava 提供的 ThreadFactoryBuilder 类来创建线程工厂了，核心线程数与 CPU 核心数相等，任务队列长度为 1024，拒绝策略AbortPolicy，**等待执行任务完毕后关闭线程池**。[github 查看完整代码](https://github.com/maoturing/concurrency_tools_practice/blob/master/src/main/java/threadpool/bestpractice/ThreadPoolBestPractice.java)，示例代码如下：
```java
public class ThreadPoolBestPractice {
    // 创建线程工厂，%d是线程编号，前面是线程自定义名称
    private static ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
            .setNameFormat("demo-pool-%d").build();
    // 获取CPU核心数
    private static int coreNum = Runtime.getRuntime().availableProcessors();

    // 1. 创建线程池，corePoolSize 为 cpu 核心数，使用自定义线程工厂
    private static ExecutorService threadPool = new ThreadPoolExecutor(coreNum, 2 * coreNum,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(1024), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());

    public static void main(String[] args) throws InterruptedException {
        System.out.println(Runtime.getRuntime().availableProcessors());

        // 2. 创建任务，打印线程名称
        Runnable task = () -> {
            String name = Thread.currentThread().getName();
            System.out.println(name);
        };

        // 3. 执行任务
        for (int i = 0; i < 20; i++) {
            threadPool.execute(task);
        }

        // 4. 等待任务执行完毕，停止线程池
        threadPool.shutdown();
        boolean isTerminated = threadPool.awaitTermination(1000L, TimeUnit.SECONDS);
        if (isTerminated) {
            // 任务执行完毕后打印"Done"
            System.out.println("Done");
        }
    }
}
``` 

// todo 此处应该补充 Spring 中创建池的方法。

https://mp.weixin.qq.com/s/z3gjfk4l-s8aKD4cvY8CHA  使用自定义线程池执行异步任务
https://mp.weixin.qq.com/s/05Ud0t7ECIYWMePhuOf6tg  使用Spring默认线程池执行异步任务

// springboot中启动异步任务@Async ，定时任务@Scheduled
https://www.bilibili.com/video/av38657363?p=95


## 2.5 停止线程池

`ThreadPoolExecutor`停止线程有 **5 个相关方法：**

- `shutdown()`  ：通知线程池停止，线程池会等已经添加的任务执行结束后停止。**最常用**，一般所有任务提交后使用该方法停止线程池，能保证任务执行完毕。
- `isShutdown()` ：查看线程池是否收到了 shutdown通知，若收到，则不能添加新任务
- `isTerminated()` ：判断线程池是否**已经停止**，与shutdown不同，shutdown是判断线程池是否开始停止，即是否收到了通知
- `awaitTermination(long timeout)) ：等待一段时间，检测线程池是否已经停止，该方法是一个死循环，若线程池停止了，返回true，若线程池未停止，则一直检测直至超时，返回false
- `shutdownNow()` ：强制停止线程池，**使用 interrupt 方法通知正在执行的线程**，并返回任务队列中的任务集合，用于记录日志或保存到数据库中。

[去Github查看停止线程池 5 种相关方法的代码示例。](https://github.com/maoturing/concurrency_tools_practice/blob/master/src/main/java/threadpool/demo/UserThreadPool.java)
这5个方法的源码分析见《Java 并发编程之美 8.3.3》

// 补充：停止线程池最佳实践


## 2.6 钩子方法与线程池监控

## 2.6.1 钩子Hook方法
钩子方法：简单的理解就是一个流程，在一个方法实现，流程中存在需要自定义的部分，则抽象出一个钩子方法，相同的部分则在流程中的实现即可。[查看Java中的钩子方法](
https://www.cnblogs.com/yanlong300/p/8446261.html)

**在每个任务执行前后都会调用钩子方法**，可以进行日志记录和统计等工作。

ThreadPoolExecutor 类中有三个个钩子方法，默认都是空实现。其中
`beforeExecute`和`afterExecute`，在每个任务执行前后都会调用这两个方法，在2.8源码分析章节 runWorker 中可以看到这两个方法在任务执行前后被调用；`terminated`是在线程池被回收前调用。使用钩子方法就是继承 ThreadPoolExecutor 类并重写这几个方法，然后就可以实现相关功能。

在2.8源码分析章节可以看到线程池执行任务 runWorker 方法前后都会调用两个钩子方法。

可以利用钩子方法实现线程池的暂停与恢复，具体[代码示例见Github。](https://github.com/maoturing/concurrency_tools_practice/blob/master/src/main/java/threadpool/PauseableThreadPool.java)  

### 2.6.2 线程池监控

线程池具有非常多的属性，比如前面提到的核心线程数，最大线程数等，线程池对于各种属性提供了访问接口：
- `getCorePoolSize()` 获取核心线程数 corePoolSize
- `getMaximumPoolSize()` 获取最大线程数 maximumPoolSize
- `getKeepAliveTime()` 获取空闲线程存活时间
- `getQueue()` 获取任务队列
- `getRejectedExecutionHandler()` 获取拒绝策略对象
- `getThreadFactory()` 获取线程工厂对象
  
前6个方法是获取 ThreadPoolExecutor 的构造参数，下面的方法是获取线程池的一些其他属性，这些属性我们会在2.8源码分析章节经常见到。
  
- `getPoolSize()`  获取线程池中线程个数
- `getLargestPoolSize()` 获取线程池中线程数出现过的最大值，创建工作线程 addWorker 方法会更新该属性 
- `getActiveCount()` 获取工作线程数，即工作线程集 workers 的大小
- `getTaskCount()`  获取任务总数，包括已完成、未完成和正在执行的任务
- `getCompletedTaskCount()`  获取已完成的任务数，在 runWorker 中线程 worker 任务执行完成会更新该属性，getCompletedTaskCount就是将线程工作集 workers 中所有线程完成任务数相加返回



## 2.7 线程池的 5 种状态
线程池一共有 5 种状态，状态转换如下图所示：
![线程池状态转换图](https://raw.githubusercontent.com/maoturing/PictureBed/master/pic/20200315005827.png)

- RUNNING：接受新任务，并且处理阻塞队列里的任务
- SHUTDOWN：拒绝新任务，但是处理阻塞队列里的任务
- STOP：拒绝新任务，并且丢弃阻塞队列里的任务
- TIDYING：整理状态，所有任务都执行完后（包括任务队列中的任务），当前线程池工作线程为0，将要调用 terminated 方法
- TERMINATED：终止状态，terminated 方法调用完后的状态

**线程池状态转换：**
- RUNNING -> SHUTDOWN：用户显式调用 shutdown() 方法，或者线程池对象被回收时隐式调用了 finalize() 中的shutdown() 方法。
- RUNNING / SHUTDOWN -> STOP：显式调用 shutdownNow() 方法
- SHUTDOWN -> TIDYING：当线程池和任务队列都为空时
- STOP -> TIDYING：当线程池为空时
- TIDYING -> TERMINATER：当 terminated() hook方法执行完成时

**源码分析：**
在 ThreadPoolExecutor 的属性定义中频繁使用位移运算来表示线程池状态，位移运算是改变当前值的一种高效手段，查看 ThreadPoolExecutor 源码可知，线程池一共有 5 种状态：
```java
// ThreadPoolExecutor 属性，用来表示线程池状态

    // 线程池默认为RUNNING状态，线程个数为0，合并起来得到线程池的ctl值
    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));

    // int值共有32位，右边29位表示工作线程数，左边3位表示线程池状态。3个二进制位可以表示0-7
    // Integer.SIZE为32，COUNT_BITS
    private static final int COUNT_BITS = Integer.SIZE - 3;

    // 000-11111111111111111111111111111,类似于子网掩码，用于位运算，使用见下方方法
    private static final int CAPACITY   = (1 << COUNT_BITS) - 1;


    // 用左边3位二进制，表示线程池状态
    // RUNNING状态，表示可以接受新任务，并且处理队列里的任务
    // -1左移29位表示 111_00000000000000000000000000000
    private static final int RUNNING    = -1 << COUNT_BITS;
    
    // SHUTDOWN状态，此状态不再接受新任务，但可以继续执行队列中的任务
    // 0左移29位表示 000_00000000000000000000000000000
    private static final int SHUTDOWN   =  0 << COUNT_BITS;
    
    // STOP状态，此状态全面拒绝任务，并中断正在处理的任务
    // 1左移29位表示 001_00000000000000000000000000000
    private static final int STOP       =  1 << COUNT_BITS;
    
    // TIDYING状态，此状态表示所有任务已经被终止
    // 2左移29位表示 010_00000000000000000000000000000
    private static final int TIDYING    =  2 << COUNT_BITS;

    // TERMINATED状态，此状态表示所有任务已经被终止
    // 3左移29位表示 011_00000000000000000000000000000
    private static final int TERMINATED =  3 << COUNT_BITS;


    // 返回线程池状态，c是线程池状态和线程数的int值，与~CAPACITY做与运算，得到左3位二进制线程池状态
    private static int runStateOf(int c)     { return c & ~CAPACITY; }

    // 返回线程池线程数，与CAPACITY做与运算，得到右29位二进制线程数
    private static int workerCountOf(int c)  { return c & CAPACITY; }

    // 把左边三位与右边29位或运算，合并成一个值
    private static int ctlOf(int rs, int wc) { return rs | wc; }
```

线程池的状态用`ctl`整型的左3位表示，五种线程池状态的十进制值大小依次为：

**RUNNING < SHUTDOWN < STOP < TIDYING < TERMINATED**

这样设计的好处是很容易通过比较值大小来判断线程池状态，例如线程池中经常需要 isRunning 判断 
```java
    // 判断线程池是否为Running状态
    private static boolean isRunning(int c) {
        return c < SHUTDOWN;
    }
```

根据以上可知，线程池调用 shutdown() 方法后会从RUNNING状态转换为SHUTDOWN状态，下面 ThreadPoolExecutor 源码中展示了线程池状态 **RUNNING -> SHUTDOWN -> TIDYING -> TERMINATED 的转换过程**，参考《并发编程之美8.3.3》。
```java
public void shutdown() {
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        checkShutdownAccess();
        // 1. 设置线程池状态为SHUTDOWN
        advanceRunState(SHUTDOWN);
        interruptIdleWorkers();
        onShutdown(); // hook for ScheduledThreadPoolExecutor
    } finally {
        mainLock.unlock();
    }
    // 尝试终止线程池
    tryTerminate();
}
```
```java
final void tryTerminate() {
    for (;;) {
        int c = ctl.get();
        // ......

        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            // 2. 设置当前线程池状态为TIDYING
            if (ctl.compareAndSet(c, ctlOf(TIDYING, 0))) {
                try {
                    // 调用钩子方法
                    terminated();
                } finally {
                    // 3. terminated方法调用完毕后，设置线程池状态为TERMINATED
                    ctl.set(ctlOf(TERMINATED, 0));
                    termination.signalAll();
                }
                return;
            }
        } finally {
            mainLock.unlock();
        }
        // else retry on failed CAS
    }
}

protected void terminated() { }
```

上面的源码线程转换过程缺少了STOP状态，下面源码展示了线程对象调用`shutdownNow()`方法后，线程状态 **RUNNING -> STOP -> TIDYING -> TERMINATED 的转换过程**。
```java
public List<Runnable> shutdownNow() {
    List<Runnable> tasks;
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        checkShutdownAccess();
        // 1. 设置线程池状态为STOP
        advanceRunState(STOP);
        interruptWorkers();
        // 将任务队列元素移出，不执行这些任务
        tasks = drainQueue();
    } finally {
        mainLock.unlock();
    }
    // 2. 设置线程池状态为TIDYING，调用terminated方法
    // 3. 设置线程池状态为TERMINATED
    tryTerminate();
    return tasks;
}
```

// ？疑问：SHUTDOWN -> TIDYING 要等待线程池和任务队列为空才行，源码中并没有看到这一点。

// 补充：mainLock的作用分析

## 2.8 实现原理与源码分析

ThreadPoolExecutor 源码有 4 个重要组成部分：
1. 线程池的各个状态，源码分析见线程池状态章节，
2. 提交任务执行是 execute() 方法，源码分析见添加线程规则章节
3. 创建并启动新线程是 addWorker()方法
4. 使用线程池中线程执行任务是 runWorker()方法

ThreadPoolExecutor 重要方法调用图如下所示，详细是嵌套调用，向右是串行执行：
![线程池方法时序图](https://raw.githubusercontent.com/maoturing/PictureBed/master/pic/20200315200326.png)

上图中 Worker 是 Runnable 接口实现类，启动线程需要 new Thread(Runnable worker).start()，而创建线程工作在 Worker 构造方法中使用线程工厂创建，创建的线程保存在 Worker 类的 final Thread thread 属性中，启动线程也是调用的 run() 方法就是 Runnable worker 的run() 方法，调用了 ThreadPoolExecutor#runWorker方法。这里比较绕，详细在 addWorker 与 runWorker 源码分析章节介绍。



### 2.8.1 创建工作线程 addWorker 源码分析

execute() 方法中创建线程调用的是 addWorker()方法，即新增工作线程 worker，代码如下：
```java
/**
 * 根据当前线程池状态，检测是否可以添加新的工作线程，如果可以则创建新线程并执行任务
 * 如果一切正常则返回true，返回false有俩种情况：
 * 1. 线程池非RUNNING状态
 * 2. 线程工厂创建新线程失败
 * @param firstTask 用户需要执行的任务Runnable，使用该任务来new 线程Thread，外部启动线程池时需要构造的第一个线程，它是线程的母体
 * @param core 新增线程时的判断指标：
 *    true 判断指标为corePoolSize，RUNNING状态线程池的线程个数小于corePoolSize则可以创建新线程
 *    false 判断指标为maximumPoolSize，RUNNING状态线程池的线程数小于maximumPoolSize则可以创建新线程
 */
private boolean addWorker(Runnable firstTask, boolean core) {
        retry:
        for (;;) {
            // 获取线程池的状态与线程个数组合值c，详细见线程状态章节
            int c = ctl.get();
            // 获取线程池状态
            int rs = runStateOf(c);

            // 1. 如果为RUNNING状态，则无法进入if语句，直接进行下一步for
            if (rs >= SHUTDOWN &&
                ! (rs == SHUTDOWN &&
                   firstTask == null &&
                   ! workQueue.isEmpty()))
                return false;

            // 2. 循环CAS增加线程个数
            for (;;) {
                // 获取线程池线程个数
                int wc = workerCountOf(c);

                // 2.1 线程数最大为2^29，否则将影响左3位的线程池状态值
                // 判断线程个数是否大于等于核心数(最大数)
                if (wc >= CAPACITY ||
                    wc >= (core ? corePoolSize : maximumPoolSize))
                    return false;
                // 2.2 CAS 增加线程数，修改ctl值，成功后跳出循环到retry标签
                if (compareAndIncrementWorkerCount(c))
                    break retry;

                // 2.3 如果CAS增加线程数失败，则继续执行内层for循环，重新CAS
                // 线程池状态线程个数值是可变化的，需要获取最新值
                c = ctl.get();  // Re-read ctl
                // 查看线程池状态是否变化，如果变化则跳出for循环到retry标签
                if (runStateOf(c) != rs)
                    continue retry;
                // else CAS failed due to workerCount change; retry inner loop
            }
        }

        // 3. 到这里说明CAS成功了，这里开始创建新的工作线程
        boolean workerStarted = false;      // 标记线程是否启动成功
        boolean workerAdded = false;        // 标记线程是否新增到workers成功
        // Worker是工作线程，实现了Runnable接口，是ThreadPoolExecutor的内部类
        Worker w = null;
        try {
            // 3.1 创建新线程，这行代码已经利用Worker构造方法中的线程池工厂创建了新线程
            // 并封装成工作线程Worker对象，线程创建时参数Runnable为firstTask
            w = new Worker(firstTask);
            
            final Thread t = w.thread;
            if (t != null) {
                final ReentrantLock mainLock = this.mainLock;
                // 3.2 添加独占锁，为了实现workers同步，因为可能多个线程同时调用了同一个线程池的execute方法
                mainLock.lock();
                try {
                    // 3.3 获取线程池状态，避免在获取锁前调用了shutdown方法
                    int rs = runStateOf(ctl.get());

                    if (rs < SHUTDOWN ||
                        (rs == SHUTDOWN && firstTask == null)) {
                        
                        // 如果线程已启动调用过start方法，则抛出异常，等价于普通线程启动状态校验
                        if (t.isAlive()) // precheck that t is startable
                            throw new IllegalThreadStateException();

                        // 3.4 添加新创建的工作线程 w 到工作线程集workers
                        workers.add(w);
                        int s = workers.size();
                        if (s > largestPoolSize)
                            largestPoolSize = s;
                        workerAdded = true;
                    }
                } finally {
                    mainLock.unlock();
                }

                // 3.5 添加成功后启动工作线程，调用线程start方法，这个工作线程是线程池工厂创建的
                if (workerAdded) {
                    // 启动线程，调用线程t的run方法，
                    // 线程t是new Thread(Runnable worker)得到的，所以会调用worker.run()方法
                    t.start();
                    // 线程启动成功
                    workerStarted = true;
                }
            }
        } finally {
            // 3.6 如果线程启动失败，则把新增的线程从workers中remove掉，然后线程个数ctl减1
            if (! workerStarted)
                addWorkerFailed(w);
        }
        return workerStarted;
    }   
```
代码较长，主要分为两个部分：1.双重循环通过 CAS 操作增加线程数；2.创建新工作线程，并线程线程安全的将新线程添加到 workers 中，并且启动新线程。

首先来分析第一段代码，通过CAS增加线程数，代码1中，下面三种情况会新增线程失败并返回 false：
1. 当前线程池状态为STOP，TIDYING和TERMINATED
2. 线程池状态为SHUTDOWN并且已经有了firstTask
3. 当前线程池状态为SHUTDOWN并且任务队列为空

内层循环的作用是使用CAS操作设置线程个数，代码2.1判断如果线程个数超限制则返回 false，否则执行代码2.2CAS操作设置线程个数，CAS成功则退出双新欢，CAS失败则执行代码2.3看线程状态是否变化，如果变了，则再次进入外层循环重新获取线程池状态，否则依旧在内存循环继续CAS尝试。

**compareAndIncrementWorkerCount()** 方法是CAS添加线程个数，修改表示线程池状态和线程个数的 ctl 值，执行失败概率非常低，类似自旋锁原理。这里的处理逻辑是线程个数先加 1，如果后面创建线程失败再减 1，这是轻量处理并发创建线程的方式。如果先创建线程，成功再加 1，当发现超出线程数限制后再销毁线程，这种处理方式明显比前者的代价要大。

第二部分的代码3说明使用CAS成功的增加了线程个数，但是现在新线程还未创建，这里使用全局的独占锁把系新增的线程 worker 添加的工作线程集 workers 中。

代码3.1使用线程工厂创建了一个新的工作线程，代码3.2获取了独占锁，代码3.3重新检查线程池状态，这是为了避免在获取锁前其他线程调用了shutdown方法关闭了线程池。如果线程池被关闭，则直接释放锁；否则执行代码3.4添加新工作线程 w 到工作线程集 workers 中，然后释放锁。代码3.5判断如果新增工作线程成功，则启动新线程。

代码3.6判断线程是否启动成功，如果启动会失败则从工作线程集 wokers 中移除新线程 w，并将线程数减 1。
```java
private void addWorkerFailed(Worker w) {
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        if (w != null)
            // 从工作线程集中移除新线程
            workers.remove(w);
        // 将线程个数减1，修改ctl值，对应compareAndIncrementWorkerCount方法
        decrementWorkerCount();
        tryTerminate();
    } finally {
        mainLock.unlock();
    }
}
```

用户提交任务到线程池后，由工作线程 Worker 来执行，Worker 的构造函数如下：
```java
// 工作线程Worker实现了Runnable接口，并把本对象作为参数传入newThread()创建新线程，
// new Thread(Runnable r)中参数r就是用户任务firstTask
private final class Worker extends AbstractQueuedSynchronizer
    implements Runnable {

    Worker(Runnable firstTask) {
        setState(-1); // 在调用runWorker前禁止中断
        this.firstTask = firstTask;
        // 使用线程工厂创建一个新线程, 并把本对象作为参数传入newThread()创建新线程
        // 线程工厂中new Thread(Runnable worker)参数就是当前worker对象
        this.thread = getThreadFactory().newThread(this);   
    }

    // 在addWorker中启动线程是Worker.thread.start()，
    // 最终还是调用worker对象的run方法，即调用runWorker()方法
    @Override
    public void run() {
        // 任务执行逻辑与线程复用
        runWorker(this);
    }
```
由上面源码可知，addWorker 中使用线程工厂创建并启动新线程，最终执行任务调用的是 Worker#runWorker 方法。

### 2.8.2 实现线程复用 runWorker 源码分析
上一小节我们知道 addWorker 创建了工作线程后，调用 Worker.thread.start() 启动线程，执行用户传入的任务则在 Worker#run 方法中，具体的执行任务逻辑和**线程复用**执行任务的实现在 runWorker 方法中，源码如下所示：


```java
final void runWorker(Worker w) {
    Thread wt = Thread.currentThread();
    // 用户提交的任务
    Runnable task = w.firstTask;
    w.firstTask = null;
    w.unlock(); // allow interrupts
    boolean completedAbruptly = true;
    try {
        // getTask从任务队列取出任务，执行任务，直至任务队列为空
        // 当前代码都在工作线程的run方法中，循环调用任务的run方法相当于在复用工作线程
        while (task != null || (task = getTask()) != null) {
            w.lock();
            // If pool is stopping, ensure thread is interrupted;
            // if not, ensure thread is not interrupted.  This
            // requires a recheck in second case to deal with
            // shutdownNow race while clearing interrupt
            if ((runStateAtLeast(ctl.get(), STOP) ||
                    (Thread.interrupted() &&
                    runStateAtLeast(ctl.get(), STOP))) &&
                !wt.isInterrupted())
                wt.interrupt();
            try {
                // 1. 调用前置钩子方法，在每个任务执行前调用
                beforeExecute(wt, task);
                Throwable thrown = null;
                try {
                    // 2. 调用用户提交任务的task的run()方法，worker串行执行，相当于worker线程一直在运行用户的任务
                    task.run();
                } catch (RuntimeException x) {
                    thrown = x; throw x;
                } catch (Error x) {
                    thrown = x; throw x;
                } catch (Throwable x) {
                    thrown = x; throw new Error(x);
                } finally {
                    // 3. 调用后置钩子方法，在每个任务执行后调用  
                    afterExecute(task, thrown);
                }
            } finally {
                task = null;
                // 4. 统计该worker线程完成任务的个数
                w.completedTasks++;
                w.unlock();
            }
        } // while

        // 如果在执行任务期间未抛出异常，则可以执行这一行代码
        completedAbruptly = false;
    } finally {
        // 5. 执行清理工作，每个worker执行多个任务，但只进行一次清理工作
        // 统计所有worker线程完成的任务总数，对当前worker进行销毁
        processWorkerExit(w, completedAbruptly);
    }
}

```
实现工作线程复用就是在while循环中不断从队列中获取任务Runnable task，然后调用task.run() 来处理用户任务，即实现了工作线程复用。

### 2.8.3 取出任务与回收超时线程 getTask

runWorker 在执行任务时，需要不断从任务队列取出任务，取出任务的逻辑则在 getTask() 方法中实现，getTask() 取出任务有 3 种情况：
1. 任务队列不为空，取出任务并返回
2. 任务队列为空，则需要判断是否需要回收超时线程，**若需要回收超时线程**，则该工作线程调用`workQueue.poll(keepAliveTime)`，等待任务队列 keepAliveTime，超时后返回null任务。
3. 任务队列为空，**若不需要回收超时线程**，则该工作线程调用`workQueue.take()`进入阻塞状态，直至任务队列有新任务出现。这也告诉了我们**当线程池没有任务时，线程池中 corePoolSize 数量的线程处于阻塞状态，有了新任务后唤醒线程到RUNNABLE状态，继续复用该线程。**

> 彩蛋：阻塞队列出队操作 take 与 poll 的区别如何记忆？
> 
> poll 中有 O，就像一个表在计时，超时后就返回null了，而 take 会一直等待

getTask() 的源码分析如下：

```java
private Runnable getTask() {
    // 判断是否对超时空闲线程进行回收，当任务队列为空时设置timeOut为true
    boolean timedOut = false; // Did the last poll() time out?

    // 从队列中取出任务，若队列为空，则CAS修改ctl变量的工作线程个数
    for (;;) {
        int c = ctl.get();
        int rs = runStateOf(c);

        // Check if queue empty only if necessary.
        if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
            decrementWorkerCount();
            return null;
        }

        // 工作线程数
        int wc = workerCountOf(c);

        // 工作线程数大于核心数，则需要超时判断
        boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;

        // 工作线程数必须大于1或工作队列为空，此时才考虑回收线程
        // 为什么这里不进行线程超时判断就减少工作线程？
        // 超时判断keepAliveTime在下面从队列取出元素时进行
        if ((wc > maximumPoolSize || (timed && timedOut))
            && (wc > 1 || workQueue.isEmpty())) {
            // ctl 工作线程数-1，具体的线程销毁工作会在processWorkerExit中执行
            if (compareAndDecrementWorkerCount(c))
                // 2. 需要回收超时线程，超时未取到任务返回null
                return null;
            continue;
        }

        try {
            // 工作线程数若大于corePoolSize，需要回收超时线程，三目表达式
            // 2. poll 是从任务队列中取出队首元素，若队列为空，则等待keepAliveTime，超时后返回null
            // 3. take 是从任务队列中取出队首元素，若队列为空，则一直阻塞，直至取出任务
            Runnable r = timed ?
                workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) :
                workQueue.take();

            if (r != null)
                // 1. 任务队列不为空，返回任务
                return r;

            // 任务队列为空，keepAliveTime时间内也没有取出元素，
            // 则表示需要线程超时判断置为true，回收空闲线程
            timedOut = true;
        } catch (InterruptedException retry) {
            timedOut = false;
        }
    }
}
```
```java
// 工作线程w执行完队列中所有任务后，执行清理统计工作
private void processWorkerExit(Worker w, boolean completedAbruptly) {
    // 工作线程执行任务未正确结束，如发生了异常等，需要将工作线程数-1
    if (completedAbruptly) 
        decrementWorkerCount();

    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        // 1.统计整个线程池完成的任务数
        completedTaskCount += w.completedTasks;
        // 2.移除工作线程w，线程数ctl值的修改在getTask任务中已经完成
        // 这里移除的线程一定是非核心线程，核心线程没有任务会阻塞，不会进行到这一步
        workers.remove(w);
    } finally {
        mainLock.unlock();
    }

    // 3. 设置线程池状态为TIDYING，调用钩子terminated方法
    // 4. 设置线程池状态为 TERMINATED
    tryTerminate();

    // 创建线程至核心数corePoolSize，存在corePoolSize=0，出现新任务的情况
    int c = ctl.get();
    if (runStateLessThan(c, STOP)) {
        if (!completedAbruptly) {
            // 该工作线程成功结束
            int min = allowCoreThreadTimeOut ? 0 : corePoolSize;
            // 最小线程数为0且任务队列不为空，说明不合适，需要将最小线程数设置为1
            // 队列为空则说明最小线程为1也可以，则不修改min
            if (min == 0 && ! workQueue.isEmpty())
                min = 1;

            // 工作线程数大于min，则返回即可
            if (workerCountOf(c) >= min)
                return; // replacement not needed
        }
        // 到这一步，说明工作线程数小于min，需要新增工作线程
        addWorker(null, false);
    }
}
```
> **面试题4：** 我们知道线程状态从RUNNABLE到TERMINATED是不可逆的，比如一个线程只能调用一次start() 方法，线程生命周期到了TERMINATED就结束了，无法重新到达RUNNABLE状态。
> 
> 那么线程池能够复用线程是怎么实现的？

通过 ThreadPoolExecutor#runWorker 源码可知，一个工作线程启动后，run 方法中循环调用用户任务 Runnable#run() 方法执行任务，相当于工作线程始终处于 RUNNABLE 状态，直至所有用户任务执行完毕，线程才会退出循环，线程到达TERMINATED状态。

> **面试题5：** 前面提到线程池中任务执行完毕后，会回收空闲线程，保留 corePoolSize 个线程。如上一问题所说，任务执行完毕后线程会到达TERMINATED状态，那如何保留工作线程呢？

通过 ThreadPoolExecutor#getTask 源码可知，在工作线程执行任务时，会从任务队列取出任务，当任务队列为空时，会调用`workQueue.take()`，使线程进入阻塞状态，当有任务时，会线性线程到 Runnable 状态继续复用线程。

### 2.8.3 ScheduledThreadPoolExecutor 的实现
ScheduledThreadPoolExecutor 的执行示意图如下所示：

DelayQueue 是一个无界队列，封装了一个优先队列 PriorityQueue，会对队列中任务按照执行时间进行排序。

## 2.9 线程池设计模式

虽然在 Java 语言中创建线程看上去就像创建一个对象一样简单，只需要 new Thread() 就可以了，但实际上创建线程远不是创建一个对象那么简单。创建对象，仅仅是在 JVM 的堆里分配一块内存而已；而创建一个线程，却需要调用操作系统内核的 API，然后操作系统要为线程分配一系列的资源，这个成本就很高了，所以线程是一个重量级的对象，应该避免频繁创建和销毁。

线程池和一般意义上的池化资源是不同的。一般意义上的池化资源如数据库连接池，都是下面这样，当你需要资源的时候就调用 acquire() 方法来申请资源，用完之后就调用 release() 释放资源。
```java
class XXXPool{
    // 获取池化资源
    XXX acquire() {......}
    // 释放池化资源
    void release(XXX x){......}
}  
```

目前业界线程池的设计，普遍采用的都是生产者 - 消费者模式。线程池的使用方是生产者，线程池本身是消费者。在下面的示例代码中，我们创建了一个非常简单的线程池 MyThreadPool，你可以通过它来理解线程池的工作原理。
```java

//简化的线程池，仅用来说明工作原理
class MyThreadPool {
    //利用阻塞队列实现生产者-消费者模式
    BlockingQueue<Runnable> workQueue;
    //保存内部工作线程
    List<WorkerThread> threads = new ArrayList<>();

    // 构造方法
    MyThreadPool(int poolSize, BlockingQueue<Runnable> workQueue) {
        this.workQueue = workQueue;
        // 创建工作线程
        for (int idx = 0; idx < poolSize; idx++) {
            WorkerThread work = new WorkerThread();
            work.start();
            threads.add(work);
        }
    }

    // 提交任务
    void execute(Runnable command) {
        workQueue.put(command);
    }

    // 工作线程负责消费任务，并执行任务
    class WorkerThread extends Thread {
        public void run() {
            //循环取任务并执行
            while (true) {
                Runnable task = workQueue.take();
                task.run();
            }
        }
    }
}


    /** 下面是使用示例 **/
    // 创建有界阻塞队列
    BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(2);
    // 创建线程池
    MyThreadPool pool = new MyThreadPool(10, workQueue);
    // 提交任务
    pool.execute(()->{System.out.println("hello");});
```

## 2.10 自己动手实现线程池

[线程池没你想的那么简单 上 - crossoverJie](https://mp.weixin.qq.com/s/tT7bfFTbAeu1u6vH4v7SMg)

[线程池没你想的那么简单 下 - crossoverJie](https://mp.weixin.qq.com/s/nXbdftxLg39L_7mU8gN3hw
)


# 待补充

ScheduledThreadPoolExecutor 与 DelayedWorkQueue 的使用示例与源码分析，参考Java并发编程之美，摘取相应方法执行图。以及优先队列 PriorityQueue 的应用

ThreadPoolExecutor#submit方法的作用

Spring 中创建池

查看上文中的 **// 补充** 标记

补充Java并发编程的艺术中线程池的方法执行图

ForkJoinPool

java8 parallelStream

# 推荐阅读
1. [Java并发编程之美 - 翟陆续](https://book.douban.com/subject/30351286/)  内容和慕课网[玩转Java并发](https://coding.imooc.com/class/chapter/409.html)类似，可以配合阅读，有丰富的源码分析，实践部分有10个小案例
   
2. [Java并发编程实战 - 极客时间](https://time.geekbang.org/column/intro/159)  内容有深度，并发设计模式，分析了 4 个并发应用案例 Guava RateLimiter，Netty，Disrupter 和 HiKariCP，还介绍了 4 种其他类型的并发模型 Actor，协程，CSP等
3. [精通Java并发编程 - 哈维尔](https://book.douban.com/subject/30327401/)  全书20+案例，几乎每个知识点和章节都有案例，现在学的是原理和api，这本书学的是使用并发解决问题
4. 传智播客8天并发  笔记有并发案例，CPU原理等笔记，非常深入，后面画时间学习一下精

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
