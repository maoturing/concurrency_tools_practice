package threadpool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 高度可伸缩线程池，新任务不经过队列直接交给线程池，线程池创建新线程来执行任务
 *  CacheThreadPool 的coreSize为0，maxSize为Integer.MAX_VALUE
 *
 */
public class CacheThreadPoolTest {
    public static void main(String[] args) {
        ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
        // 可以看到很少有重复的线程，有新任务都创建新线程或使用空闲线程来执行，不会保存在任务队列
        for (int i = 0; i < 1000 ; i++) {
            cachedThreadPool.execute(new Task());
        }
    }
}
