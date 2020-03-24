package lock.lock;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 乐观锁演示
 */
public class OptimisticLock {
    int a = 6;
    // 乐观锁
    public synchronized void test() {
        AtomicInteger atomicInteger = new AtomicInteger(a);
        atomicInteger.incrementAndGet();
    }

    // 悲观锁
    public synchronized void test2() {
        a++;
    }
}
