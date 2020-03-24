package lock.reentrantlock;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 演示ReentrantLock可重入的性质
 * 可重入锁指的是同一个线程再次获取自己已经持有的锁
 */
public class ReentrantLockDemo {
    public static ReentrantLock lock = new ReentrantLock();

    public static void main(String[] args) {
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
