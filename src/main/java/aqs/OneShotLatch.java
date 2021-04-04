package aqs;


import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * 自己用 AQS 实现一个简单的线程协作工具: 一次性门闩
 * <p>
 * 一次性门闩: 默认门闩关闭, 线程调用 await() 进入等待状态, 可能会有多个线程在这等待
 * 直至有一个线程调用 signal() 将门闩打开, 那么将唤醒之前等待的所有线程
 *
 * 后面再加入对中断信号的传递处理, 参考java并发编程艺术 p121
 * @author mao  2021/3/30 6:51
 */
public class OneShotLatch {
    private final Sync sync = new Sync();

    public static void main(String[] args) throws InterruptedException {
        OneShotLatch latch = new OneShotLatch();
        for (int i = 0; i < 10; i++) {
            final int j = i;
            new Thread(() -> {
                System.out.println("线程" + j + " 尝试获取 latch...");
                latch.await();

                System.out.println("线程" + j + "被唤醒，继续执行业务逻辑....");
            }).start();
        }
        TimeUnit.SECONDS.sleep(5);

        // 打开门闩放行, 唤醒所有等待线程
        System.out.println("======== 打开门闩放行 =======");
        latch.signal();

        // 因为是 一次性门闩, 所以前面打开门闩放行之后, 后面来的线程并不会被阻塞
        new Thread(() -> {
            System.out.println("线程10尝试获取 latch...");
            latch.await();

            System.out.println("线程10被唤醒，继续执行业务逻辑....");
        }).start();
    }

    // 1.1 获取锁方法
    public void await() {
        // 调用方法1
        sync.acquireShared(0);
    }

    // 1.2 释放锁方法，打开门闩
    public void signal() {
        // 调用方法3
        sync.releaseShared(0);
    }

    // 2 内部写一个Sync继承AQS
    class Sync extends AbstractQueuedSynchronizer {

        // 3.1 根据是否独占来重写tryAcquire()/tryAcquireShared()
        // 方法2, 尝试通过, 根据state的值判断门闩是否打开
        @Override
        protected int tryAcquireShared(int arg) {
            int state = getState();

            // 等于0表示门闩关闭，返回 -1 表示将当前线程加入等待队列
            // 不等于1表示门闩打开,返回 1 表示当前线程可以继续执行
            return state == 0 ? -1 : 1;
        }

        /**
         * 3.1 根据是否独占来重写tryRelease()/tryReleaseShared()
         * 方法4, 打开门闩, 修改state为 1
         */
        @Override
        protected boolean tryReleaseShared(int arg) {
            int state = getState();

            // 将 state值修改为 1, CAS是为了防止多个线程同时打开门闩
            if (compareAndSetState(state, 1)) {

                // 返回true表示打开门闩放行,所有线程都可以继续执行,
                return true;
            }
            return false;
        }
    }
}
