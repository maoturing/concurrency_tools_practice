package lock.reentrantlock;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 使用ReentrantLock可重入的性质进行递归文件处理
 */
public class RecursionDemo {
    public static ReentrantLock lock = new ReentrantLock();

    public static void main(String[] args) {
        accessResource();
    }

    private static void accessResource() {
        lock.lock();
        System.out.println("资源被处理了" + lock.getHoldCount() + "次");
        try {
            if (lock.getHoldCount() < 5) {
                accessResource();       // idea左侧符号是递归符号
            }

        } finally {
            lock.unlock();
        }
    }
}
