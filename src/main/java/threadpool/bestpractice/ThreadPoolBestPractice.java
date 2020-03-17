package threadpool.bestpractice;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.*;

/**
 * 创建线程池最佳实践
 */
public class ThreadPoolBestPractice {
    // 创建线程工厂，%d是线程编号，前面是线程自定义名称
    private static ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
            .setNameFormat("demo-pool-%d").build();
    // 获取CPU核心数
    private static int coreNum = Runtime.getRuntime().availableProcessors();

    // 1. 创建线程池，corePoolSize 为 cpu 核心数，使用自定义线程工厂
    private static ExecutorService threadPool = new ThreadPoolExecutor(coreNum, 2 * coreNum,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(1024), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());

    public static void main(String[] args) throws InterruptedException {
        System.out.println(Runtime.getRuntime().availableProcessors());

        // 2. 创建任务，打印线程名称
        Runnable task = () -> {
            String name = Thread.currentThread().getName();
            System.out.println(name);
        };

        // 3. 执行任务
        for (int i = 0; i < 20; i++) {
            threadPool.execute(task);
        }

        // 4. 等待任务执行完毕，停止线程池
        threadPool.shutdown();
        boolean isTerminated = threadPool.awaitTermination(1000L, TimeUnit.SECONDS);
        if (isTerminated) {
            // 任务执行完毕后打印"Done"
            System.out.println("Done");
        }
    }
}
