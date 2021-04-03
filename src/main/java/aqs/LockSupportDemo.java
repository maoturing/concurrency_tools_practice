package aqs;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 测试wait/notify方法
 * <p>
 * 这里参考的尚硅谷面试第3季 p11
 *
 * @author mao  2021/3/28 2:37
 */
public class LockSupportDemo {
    static Object objectLock = new Object();
    static Lock lock = new ReentrantLock();
    static Condition condition = lock.newCondition();

    public static void main(String[] args) {
//        waitNotifyTest();
//        waitNotifyTest2();
//        waitNotifyTest3();
//        awaitSinalTest();
//        awaitSinalTest2();
//        parkTest();
//        parkTest2();

//        LockSupportDemo testPark = new LockSupportDemo();
//        testPark.parkBlockTest();

//        waitInterruptTest();
        parkInterruptTest();

    }

    /**
     * unpark 最多将 permit 置为1, 所以这个例子是无法正确唤醒的
     */
    private static void parkTest2() {
        Thread t1 = new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("t1 come in");

            // 阻塞线程, 等待唤醒
            LockSupport.park();
            System.out.println("t1 被唤醒 1 次");
            LockSupport.park();         // 不会被唤醒, 线程一直挂起
            System.out.println("t1 被唤醒 2 次");       // 不会被执行
        }, "t1");

        t1.start();

        new Thread(() -> {
            // 唤醒线程 t1
            LockSupport.unpark(t1);
            System.out.println("通知唤醒 t1 1 次");

            // 唤醒线程 t1
            LockSupport.unpark(t1);
            System.out.println("通知唤醒 t1 2 次");
        }, "t2").start();
    }


    /**
     * park/unpark示例
     * 不需要在同步块中调用, 就可以实现线程阻塞和唤醒
     * 即使先唤醒 unpark,后阻塞park, 也能正确唤醒线程.
     */
    private static void parkTest() {
        Thread t1 = new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(Thread.currentThread().getName() + "\t ----- come in");

            // 阻塞线程, 等待唤醒
            LockSupport.park();
            System.out.println(Thread.currentThread().getName() + "\t ----- 被唤醒");
        }, "t1");

        t1.start();

        new Thread(() -> {
            // 唤醒线程 t1
            LockSupport.unpark(t1);
            System.out.println(Thread.currentThread().getName() + "\t ----- 通知唤醒");
        }, "t2").start();
    }

    /**
     * park(blocker) 使用示例
     * 当线程阻塞后, 使用jconsole 或 jstack 可以是哪个对象将线程阻塞
     */
    public void parkBlockTest() {
        Thread t1 = new Thread(() -> {
            System.out.println("t1 come in");

            // 阻塞线程, 等待唤醒, 设置blocker
            LockSupport.park(this);
        }, "t1");

        t1.start();
    }

    /**
     * park 挂起阻塞线程后, 发送interrupt信号, 线程被唤醒继续执行
     * 与 wait() 挂起阻塞线程后, 发送interrupt信号, 线程被唤醒并抛出异常
     */
    public static void parkInterruptTest() {
        Thread t1 = new Thread(() -> {
            System.out.println("t1 thread begin park...");

            // 阻塞线程
            LockSupport.park();

            // 输出中断标志位
            System.out.println(Thread.currentThread().isInterrupted());
            System.out.println("t1 thread continue  ...");
        }, "t1");
        t1.start();

        try {TimeUnit.SECONDS.sleep(2);} catch (InterruptedException e) { e.printStackTrace();}

        // 发送中断信号
        t1.interrupt();
    }

    /**
     * wait() 挂起阻塞线程后, 发送interrupt信号, 线程被唤醒并抛出异常
     */
    public static void waitInterruptTest() {
        Thread t1 = new Thread(() -> {
            System.out.println("t1 thread begin wait...");

            synchronized (objectLock) {
                try {
                    // 阻塞线程
                    objectLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // 打印中断标志位
            System.out.println(Thread.currentThread().isInterrupted());
            System.out.println("t1 thread continue  ...");
        }, "t1");
        t1.start();

        try {TimeUnit.SECONDS.sleep(2);} catch (InterruptedException e) { e.printStackTrace();}

        // 发送中断信号
        t1.interrupt();
    }

    /**
     * condition.await/signal 的正确示例
     * 必须在lock锁内使用, 否则会报 IllegalMonitorStateException
     * 因为wait是释放锁的, 当然要保证当前线程先获取了对象锁 monitor
     */
    private static void awaitSinalTest() {
        new Thread(() -> {
            // 加锁
            lock.lock();

            try {
                System.out.println(Thread.currentThread().getName() + "\t ----- come in");
                // 释放锁阻塞线程
                condition.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                // 释放锁
                lock.unlock();
            }
            System.out.println(Thread.currentThread().getName() + "\t ----- 被唤醒");
        }, "t1").start();

        new Thread(() -> {
            // 加锁
            lock.lock();

            try {
                // 唤醒其他线程
                condition.signal();
                System.out.println(Thread.currentThread().getName() + "\t ----- 通知唤醒");
            } finally {
                // 释放锁
                lock.unlock();
            }
        }, "t2").start();
    }

    /**
     * condition.await/signal 的错误示例
     * signal必须在await前调用, 否则线程t1会一直被挂起, 无人唤醒
     */
    private static void awaitSinalTest2() {
        new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // 加锁
            lock.lock();
            try {
                System.out.println(Thread.currentThread().getName() + "\t ----- come in");
                // 释放锁阻塞线程
                condition.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                // 释放锁
                lock.unlock();
            }
            System.out.println(Thread.currentThread().getName() + "\t ----- 被唤醒");
        }, "t1").start();

        new Thread(() -> {
            // 加锁
            lock.lock();

            try {
                // 唤醒其他线程
                condition.signal();
                System.out.println(Thread.currentThread().getName() + "\t ----- 通知唤醒");
            } finally {
                // 释放锁
                lock.unlock();
            }
        }, "t2").start();
    }

    /**
     * wait/notify 正确的使用示例
     */
    private static void waitNotifyTest() {
        new Thread(() -> {
            synchronized (objectLock) {
                System.out.println(Thread.currentThread().getName() + "\t ----- come in");
                try {
                    // 释放锁阻塞线程
                    objectLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread().getName() + "\t ----- 被唤醒");
            }
        }, "t1").start();

        new Thread(() -> {
            synchronized (objectLock) {
                // 唤醒其他线程
                objectLock.notify();
                System.out.println(Thread.currentThread().getName() + "\t ----- 通知唤醒");
            }
        }, "t2").start();
    }

    /**
     * wait/notify 必须在 synchronized代码块中执行, 否则会报IllegalMonitorStateException
     * 因为wait是释放锁的, 当然要保证当前线程先获取了对象锁 monitor
     */
    private static void waitNotifyTest2() {
        new Thread(() -> {
            System.out.println(Thread.currentThread().getName() + "\t ----- come in");
            try {
                // 释放锁阻塞线程
                objectLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(Thread.currentThread().getName() + "\t ----- 被唤醒");
        }, "t1").start();

        new Thread(() -> {
            // 唤醒其他线程
            objectLock.notify();
            System.out.println(Thread.currentThread().getName() + "\t ----- 通知唤醒");
        }, "t2").start();
    }

    /**
     * 执行notify后wait, 线程t1会一直被挂起, 没人唤醒
     */
    private static void waitNotifyTest3() {
        new Thread(() -> {
            try {
                // 休眠2秒, 先执行notify后wait
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            synchronized (objectLock) {
                System.out.println(Thread.currentThread().getName() + "\t ----- come in");
                try {
                    // 释放锁阻塞线程
                    objectLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread().getName() + "\t ----- 被唤醒");
            }
        }, "t1").start();

        new Thread(() -> {
            synchronized (objectLock) {
                // 唤醒其他线程
                objectLock.notify();
                System.out.println(Thread.currentThread().getName() + "\t ----- 通知唤醒");
            }
        }, "t2").start();
    }
}
