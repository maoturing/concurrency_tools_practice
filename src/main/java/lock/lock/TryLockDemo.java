package lock.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 使用tryLock避免死锁
 */
public class TryLockDemo {
    public static final Lock lock1 = new ReentrantLock();
    public static final Lock lock2 = new ReentrantLock();

    public static void main(String[] args) {
        int flag = 1;
        Runnable task1 = () -> {
            try {
                if (lock1.tryLock(1, TimeUnit.SECONDS)) {
                    try {
                        System.out.println("获取了lock1");
                        TimeUnit.SECONDS.sleep(1);
                    } finally {
                        lock1.unlock();
                    }
                } else {
                    System.out.println("没有获取到lock1");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("获取了lock1");
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            lock2.lock();
            System.out.println("获取到了lock2");
            lock2.unlock();
        };
    }
}
