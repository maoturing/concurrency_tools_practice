package lock.reentrantlock;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 自己动手利用可重入锁ReentrantLock实现一个线程安全的List
 * @author mao  2020/3/28 6:35
 */
public class ReentrantLockList {
    private ArrayList<String> array = new ArrayList<>();
    private volatile ReentrantLock lock = new ReentrantLock();

    public void add(String e) {
        lock.lock();
        try {
            array.add(e);
        }finally {
            lock.unlock();
        }
    }
    public void remove(String e) {
        lock.lock();
        try {
            array.remove(e);
        }finally {
            lock.unlock();
        }
    }

    // 为了防止其他线程正在写入时，读取数据可能会读不到正在写入的数据，加锁保证数据一致
    // 但是也会导致多个线程读时也要获取锁，降低了效率
    public String get(int index) {
        lock.lock();
        try {
            return array.get(index);
        }finally {
            lock.unlock();
        }
    }
}
