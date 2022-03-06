
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
