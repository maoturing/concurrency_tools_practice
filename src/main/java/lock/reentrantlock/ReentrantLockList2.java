package lock.reentrantlock;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 自己动手利用读写锁ReentrantReadWriteLock实现一个线程安全的List
 * 相比可重入锁，这个效率更高，在get时使用读锁，提高了效率
 *
 * @author mao  2020/3/28 6:35
 */
public class ReentrantLockList2 {
    private ArrayList<String> array = new ArrayList<>();
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    public void add(String e) {
        writeLock.lock();
        try {
            array.add(e);
        } finally {
            writeLock.unlock();
        }
    }

    public void remove(String e) {
        writeLock.lock();
        try {
            array.remove(e);
        } finally {
            writeLock.unlock();
        }
    }

    // 多个线程可以一起读，有线程写时，等待写完再读
    public String get(int index) {
        readLock.lock();
        try {
            return array.get(index);
        } finally {
            readLock.unlock();
        }
    }
}
