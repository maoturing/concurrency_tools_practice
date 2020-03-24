package lock.reentrantlock;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 演示ReentrantLock可重入的性质
 * 可重入锁指的是同一个线程再次获取自己已经持有的锁
 */
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
