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
        // 1,2可以同时读，读锁是共享锁
        new Thread(()->readText(), "user1").start();
        new Thread(()->readText(), "user2").start();
        // 3要获取写锁，需要前面释放读锁
        new Thread(()->writeText(), "user3").start();
        // 4要获取写锁，需要等待3释放写锁，写锁是排他锁
        new Thread(()->writeText(), "user4").start();
        // 5需要获取读锁，但是读锁不能插写锁的队，所以5要等待4执行完
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
