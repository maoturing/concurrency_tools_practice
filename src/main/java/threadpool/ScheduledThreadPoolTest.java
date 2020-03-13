package threadpool;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * ScheduledThreadPool 线程池示例
 */
public class ScheduledThreadPoolTest {

    public static void main(String[] args) {
        ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(10);

        // 1. 延迟执行，5s后开始执行任务
        scheduledThreadPool.schedule(new Task(), 5, TimeUnit.SECONDS);

        // 2. 定期执行，1s后开始执行，3s定期执行一次
        scheduledThreadPool.scheduleAtFixedRate(new Task(), 1, 3, TimeUnit.SECONDS);
    }
}
