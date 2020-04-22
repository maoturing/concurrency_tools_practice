package lock.spinlock;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 自己实现自旋锁
 *
 * @author mao  2020/3/27 7:34
 */
public class SpinLock {
    private AtomicReference<Thread> sign = new AtomicReference<>();

    public static void main(String[] args) {
        SpinLock spinLock = new SpinLock();
        Runnable r = () -> {
            System.out.println(Thread.currentThread().getName() + "开始尝试获取自旋锁");
            spinLock.lock();
            System.out.println(Thread.currentThread().getName() + "获取到了自旋锁");
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                spinLock.unLock();
                System.out.println(Thread.currentThread().getName() + "释放了自旋锁");
            }
        };
        new Thread(r, "Thread1").start();
        new Thread(r, "Thread2").start();
    }

    public void lock() {
        Thread current = Thread.currentThread();

        // 如果原子引用sign为null，则将其修改为当前线程，表示当前线程获取到了锁
        while (!sign.compareAndSet(null, current)) {
            System.out.println("自旋获取失败，再次尝试");
        }
    }

    public void unLock() {
        Thread current = Thread.currentThread();
        sign.compareAndSet(current, null);
    }
}
