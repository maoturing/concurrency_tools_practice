package lock.readwrite;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 描述：     演示非公平和公平的ReentrantReadWriteLock读锁的插队策略
 * 公平锁：先到先得，不允许插队
 * 非公平锁：请求读锁的线程，在等待队列头部不是请求写线程时可以插队，而不是去唤醒队列中等待的读线程，这与非公平锁一致
 */
public class NonfairBargeDemo {

    // false表示非公平锁，公平锁会依次获取并释放锁，先到先得
    private static ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock(
            false);

    private static ReentrantReadWriteLock.ReadLock readLock = reentrantReadWriteLock.readLock();
    private static ReentrantReadWriteLock.WriteLock writeLock = reentrantReadWriteLock.writeLock();

    private static void read() {
        System.out.println(Thread.currentThread().getName() + "开始尝试获取读锁");
        readLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + "得到读锁，正在读取");
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } finally {
            System.out.println(Thread.currentThread().getName() + "释放读锁");
            readLock.unlock();
        }
    }

    private static void write() {
        System.out.println(Thread.currentThread().getName() + "开始尝试获取写锁");
        writeLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + "得到写锁，正在写入");
            try {
                Thread.sleep(40);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } finally {
            System.out.println(Thread.currentThread().getName() + "释放写锁");
            writeLock.unlock();
        }
    }

    public static void main(String[] args) {
        // 线程1持有写锁，其他的4+1000个线程都阻塞挂起并加入等待队列
        new Thread(() -> write(), "Thread1").start();
        // 等待线程1释放读锁
        new Thread(() -> read(), "Thread2").start();
        new Thread(() -> read(), "Thread3").start();
        new Thread(() -> write(), "Thread4").start();
        new Thread(() -> read(), "Thread5").start();

        // 创建1000个读线程，验证这些线程能否插队
        // 由于等待队列头元素不是请求写锁，所以下面的读线程可以插队
        // 打印结果会显示Thread1释放写锁后，下面的子线程比Thread2更早获得读锁
        new Thread(() -> {
            Thread thread[] = new Thread[1000];
            for (int i = 0; i < 1000; i++) {
                thread[i] = new Thread(() -> read(), "子线程创建的Thread" + i);
            }
            for (int i = 0; i < 1000; i++) {
                thread[i].start();
            }
        }).start();
    }
}
