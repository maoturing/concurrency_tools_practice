<!-- TOC -->

- [0. 前言](#0-前言)
- [1. 总览并发工具](#1-总览并发工具)
- [2. 线程池 - 线程治理最大法宝](#2-线程池---线程治理最大法宝)
    - [1.1 什么是线程池？](#11-什么是线程池)
    - [1.2 创建和停止线程池](#12-创建和停止线程池)
    - [1.3 常见4种线程池](#13-常见4种线程池)
    - [1.4 任务拒绝策略](#14-任务拒绝策略)
    - [1.5 钩子方法](#15-钩子方法)
    - [1.6 线程池实现原理与源码分析](#16-线程池实现原理与源码分析)
- [ThreadLocal](#threadlocal)
- [锁](#锁)
- [原子类atomic](#原子类atomic)
- [CAS](#cas)
- [不可变final](#不可变final)
- [并发集合](#并发集合)
- [并发工具类](#并发工具类)
- [AQS](#aqs)
- [Future](#future)
- [实战：打造高性能缓存](#实战打造高性能缓存)
- [参考文档与推荐阅读](#参考文档与推荐阅读)

<!-- /TOC -->

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

并发工具类根据功能可分为三大类：
1. 并发安全：
   1. 从底层原理分类：互斥同步（锁）、非互斥同步（atomic）、无同步方案（final）
   2. 从使用角度分类：限制共享变量，避免共享变量，成熟并发工具
2. 管理线程：线程池
3. 线程协作：三大并发工具类等

更加详细的分类参考[思维导图](http://naotu.baidu.com/file/ab389987308c34fdc57beb911cd0eb80?token=39caec33969b1e00)的建立并发知识框架分支

# 2. 线程池 - 线程治理最大法宝

线程池是并发工具中用来**管理线程**的工具。

码农翻身的这篇[小白入门线程池](https://mp.weixin.qq.com/s/qzoLgNNSZD2NrzBEINVuUg)文章对线程池有一个大致的概念和介绍，推荐阅读。

## 1.1 什么是线程池？

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


问题：线程不能重复 start，为什么线程池线程可以复用？
线程重复 start，会报 IllegalThreadStateException 异常，因为线程声明周期就是从 start 到 terminated，没有办法从 terminated 恢复到 start

## 1.2 创建和停止线程池
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
| Handler       | RejectedExecutionHandler | 线程池无法接受提交的任务时的拒绝策略              |

- `corePoolSize` 常驻核心线程数，如果等于0，则任务执行完毕，会销毁线程池所有线程；如果大于0，任务执行完毕核心线程不会销毁。这个值的设置非常关键，过大会浪费资源，过小灰导致线程被频繁创建和销毁。
- `maximumPoolSize` 最大线程数，表示线程池能够容纳同时执行的最大线程数，必须大于等于1。如果待执行的线程数大于此值，则缓存在任务队列 workQueue 中。如果 maxPoolSize 等于 corePoolSize，即是固定大小线程池

- `workQueue` 任务队列，当线程数大于等于 corePoolSize，则新任务保存到 BlockingQueue 阻塞队列。阻塞队列是线程安全的，使用两个 ReentrantLock 锁来保证从出队和入队的原子性，是一个生产消费模型队列

**线程池添加线程规则**
---


任务提交到线程池，添加线程规则如下：

可记为：**一cool二queue三max最后 reject**。

1. 如果线程数小于 corePoolSize，即使其他线程处于空闲状态，也会**创建一个新线程来执行新任务**。
2. 如果线程数等于（或大于）corePoolSize 但小于 maxPoolSize，则**将任务放入队列**
3. 如果队列已满，且线程数小于 maxPoolSize，则**创建一个新线程来执行任务**
4. 如果队列已满，且线程数等于 maxPoolSize，则**使用拒绝策略拒绝该任务**

**比喻：** 线程池中的线程就像**烧烤店的餐桌**，店里面的餐桌数 corePoolSize，坐满后让顾客在排队区 workQueue 排队等待；如果顾客少店内坐不满，店内餐桌也不会收起来；

如果排队区也满了，那只能冒着被城管罚款的风险在店外面广场加桌子，广场面积有限，最大餐桌数为 maxPoolSize；如果广场的顾客吃完了，那抓紧把桌子收起来；

如果排队区和广场都坐满了，那来了新顾客只能拒绝服务 rejected 了。 

![添加线程规则](https://raw.githubusercontent.com/maoturing/PictureBed/master/pic/20200307235727.png)

**源码分析：** 
```java
// ThreadPoolExecutor 源码
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
    int c = ctl.get();
    // 线程数小于常驻和弦线程数，使用addWorker创建新线程，传入任务command
    if (workerCountOf(c) < corePoolSize) {
        if (addWorker(command, true))
            return;
        c = ctl.get();
    }
    // 如果线程数大于等于coresize，则添加到队列
    // 添加成功后返回true，再次检查是否需要添加一个线程(因为入队过程中可能有线程被销毁core=0 keeptime=0)，如果isRunning返回false则从队列移除任务command，并执行拒绝策略？？？
    // 如果isRunning返回true，则添加新线程？？？
    if (isRunning(c) && workQueue.offer(command)) {
        int recheck = ctl.get();
        if (! isRunning(recheck) && remove(command))
            reject(command);
        else if (workerCountOf(recheck) == 0)
            addWorker(null, false);
    }
    // 如果线程数大于等coresize，且队列已满，则创建新线程，
    // 参数false表示线程数要小于maxsize，创建成功返回true，失败则执行拒绝策略
    else if (!addWorker(command, false))
        reject(command);
}
```


> 问题：线程池线程数能大于maxPoolSize吗？
> 问题：线程池没有任务时会怎么样？

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
- `keepAliveTime` 线程空闲时间，单位为 TimeUnit。当线程空闲时间达到 keepAliveTime 时，**线程会被回收销毁，直到只剩下 corePoolSize 个线程为止**，避免浪费内存和句柄资源？。默认情况下，线程数大于 corePoolSize时，keepAliveTime 才会起作用。
- `threadFactory` 线程工厂。用来创建线程，默认使用 Executors.defaultThreadFactory()，创建出来的线程都属于同一个线程组，相同优先级NORM_PRIORITY，都不是守护线程，线程名称形如 pool-poolNumber-trhead-id，自定义线程工厂可以设置更加友好易读的线程名称。
```java
// Executors 内部类 DefaultThreadFactory源码
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
上述的Executors 默认的线程工厂过于简单，对用户不够友好，线程名称必须明确表示，方便后期调试和分析，以下为简单的自定义线程工厂：
```java
```




手动创建与自动创建


线程数量设置

停止线程

推荐创建方式
spring  阿里巴巴推荐等 guava

自己动手实现线程池
```java
线程池中的Worker线程：
public class WorkerThread extends Thread {

    private BlockingQueue<Task> taskQueue = null;
    private boolean       isStopped = false;
    //持有一个BlockingQueue的实例
    public WorkerThread(BlockingQueue<Task> queue){
        taskQueue = queue;
    }

    public void run(){
        while(!isStopped()){
            try{
                Task task = taskQueue.take();
                task.execute();
            } catch(Exception e){
                //log or otherwise report exception,
                //but keep pool thread alive.
            }
        }
    }
    // ......
}

```

## 1.3 常见4种线程池
## 1.4 任务拒绝策略
## 1.5 钩子方法
## 1.6 线程池实现原理与源码分析






# ThreadLocal

使用场景1：每个线程需要一个独享的对象，通常是工具类，典型需要使用的类有SimpleDateFromat和Random。

使用场景2：每个线程内急需要保存全局变量，例如在拦截器中获取用户信息，可以染不同方法直接使用，避免参数传递的麻烦





# 锁

# 原子类atomic

# CAS

# 不可变final

# 并发集合

# 并发工具类

# AQS

# Future

# 实战：打造高性能缓存







# 参考文档与推荐阅读
1. [玩转Java并发工具，精通JUC - 慕课网](https://coding.imooc.com/class/chapter/409.html)
2. [并发在线思维导图](http://naotu.baidu.com/file/ab389987308c34fdc57beb911cd0eb80?token=39caec33969b1e00)
3. 传智播客8天并发  笔记有并发案例，CPU原理等精
4. 周阳JUC  
5. Java并发编程之美 - 翟陆续
6. 实战Java高并发程序设计 - 葛一鸣
7. [小白科普：线程和线程池 - 码农翻身](https://mp.weixin.qq.com/s/qzoLgNNSZD2NrzBEINVuUg)















