package threadpool.demo;

import java.util.concurrent.*;

/**
 * 用户手动创建线程池，使用之前创建的拒绝策略和线程工厂
 *
 */
public class UserThreadPool {
    /**
     * 执行main方法会打印如下日志：
     *  UserThreadFactory's 第1机房-Worker-1       // 线程工程创建线程，打印线程名称
     *  UserThreadFactory's 第1机房-Worker-2
     *  running_0                                 // task内容，打印任务执行次数
     *  running_1
     *  running_2
     *  running_3
     *  you task is rejected. count=158. java.util.concurrent.ThreadPoolExecutor@677327b6[Running, pool size = 2, active threads = 2, queued tasks = 2, completed tasks = 4]
     *      // 打印线程池状态，线程池线程数量为2，达到了我们定义的maxPoolSize=2，任务队列数量为2，达到了我们定义的workQueue容量，完成的任务数completed tasks有4个。
     *      // 到最终日志，任务的执行次数 running_和任务拒绝次数rejectCount相加等于总任务数200
     *      // 有时 queued tasks 不一定等于2，因为执行拒绝策略时队列元素为2，打印时队列元素可能已经被取走执行了，复现时可以删除 threadpool.demo.Task 类的 sleep方法。
     *
     */
    public static void main(String[] args) {
        // 1. 任务队列，避免误无解队列，防止OOM异常
        BlockingQueue workQueue = new LinkedBlockingQueue(2);

        // 2. 线程工厂，定义合适的线程名称
        UserThreadFactory f1 = new UserThreadFactory("第1机房");

        // 3. 拒绝策略
        UserRejectHandler handler = new UserRejectHandler();
        
        // 4.1 创建线程池，使用自定义线程工厂和拒绝策略
        ThreadPoolExecutor threadPool1 = new ThreadPoolExecutor(1, 2, 60, TimeUnit.SECONDS, workQueue, f1, handler);
        // 4.2 使用默认的线程工厂和拒绝策略，使用大多数情况，这里的拒绝策略时AbortPolicy，即丢弃任务并抛出异常
        ThreadPoolExecutor threadPool2 = new ThreadPoolExecutor(1, 2, 60, TimeUnit.SECONDS, workQueue);

        // 使用线程池，
        Runnable task= new Task();
        for (int i = 0; i < 200; i++) {
            // 使用自定义默认策略，拒绝任务并打印线程池状态
            // 任务的执行次数 running_和任务拒绝次数rejectCount之和应为总任务数200
            threadPool1.execute(task);

            //threadPool2.execute(task);    // 使用默认的拒绝策略，拒绝任务，并抛出异常
        }
    }
}

