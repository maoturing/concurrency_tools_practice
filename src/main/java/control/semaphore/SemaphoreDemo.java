package control.semaphore;

import java.util.concurrent.*;

/**
 * 最多3个线程获得许可证
 * @author mao  2021/3/31 13:21
 */
public class SemaphoreDemo {
    // 公平锁, 许可证数量为3
    public static Semaphore semaphore = new Semaphore(3, true);

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        for (int i = 0; i < 20; i++) {
            executorService.execute(new Task());
        }
        executorService.shutdown();
    }

    static class Task implements Runnable {
        @Override
        public void run() {
            try {
                // 获取许可证
                semaphore.acquire();
                System.out.println(Thread.currentThread().getName() + "拿到了许可证");

                // 模拟业务操作
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(Thread.currentThread().getName() + "释放了许可证");
            // 释放许可证
            semaphore.release();
        }
    }
}
