package lock.lock;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * lockInterruptibly尝试获取锁，获取锁过程中允许被中断，中断后抛出InterruptedException
 */
public class LockInterruptibly {
    private static Lock lock = new ReentrantLock();

    public static void main(String[] args) throws InterruptedException {
        Runnable r = () -> {
            System.out.println(Thread.currentThread().getName() + "尝试获取锁");
            try {
                // 获取锁
                lock.lockInterruptibly();
                try {
                    System.out.println(Thread.currentThread().getName() + "获取到了锁");

                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    System.out.println(Thread.currentThread().getName() + "睡眠期间被中断了");
                } finally {
                    // 为什么在内层finally释放锁？
                    // 因为获取锁成功才能进入内层try，如果获取锁过程中被中断，是不需要解锁的
                    lock.unlock();
                    System.out.println(Thread.currentThread().getName() + "释放了锁");
                }
            } catch (InterruptedException e) {
                System.out.println(Thread.currentThread().getName() + "等待锁期间被中断了");
            }
        };

        // 线程0先启动，获取到了锁，线程1等待获取锁
        Thread thread0 = new Thread(r);
        Thread thread1 = new Thread(r);
        thread0.start();
        thread1.start();

        Thread.sleep(2000);
        // 中断线程1
        thread1.interrupt();
    }
}
