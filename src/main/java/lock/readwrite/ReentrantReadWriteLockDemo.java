package lock.readwrite;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 读写锁的使用
 * @author mao  2020/3/26 17:10
 */
public class ReentrantReadWriteLockDemo {
    private static ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
    private static ReentrantReadWriteLock.ReadLock readLock = reentrantReadWriteLock.readLock();
    private static ReentrantReadWriteLock.WriteLock writeLock = reentrantReadWriteLock.writeLock();

    public static void main(String[] args) {
        // 1,2可以同时读，但是3,4不能同时写，有人写的时候也不能读
        new Thread(()->readText(), "user1").start();
        new Thread(()->readText(), "user2").start();
        new Thread(()->writeText(), "user3").start();
        new Thread(()->writeText(), "user4").start();
        new Thread(()->readText(), "user5").start();
    }
    private static void readText() {
        readLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + "得到读锁,正在读取...");
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println(Thread.currentThread().getName() + "释放读锁");
            readLock.unlock();
        }
    }

    private static void writeText() {
        writeLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + "得到写锁,正在写入...");
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println(Thread.currentThread().getName() + "释放写锁");
            writeLock.unlock();
        }
    }

}
