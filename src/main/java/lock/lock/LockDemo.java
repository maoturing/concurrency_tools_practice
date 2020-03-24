package lock.lock;

import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * lock() unlock() 使用演示
 * <p>
 * Lock 不会像synchronized一样，自动释放锁，即使发生异常，也能正确释放
 * 所以使用 Lock 必须在finally中手动释放锁
 */
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
            lock.unlock();
        }
    }
}
