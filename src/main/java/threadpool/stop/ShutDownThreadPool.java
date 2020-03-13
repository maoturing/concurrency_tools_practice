package threadpool.stop;



import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 停止线程池，使用shutdown()方法
 * shutdown会等待所有任务都执行完后再停止线程池，此时不能再向线程池添加新任务，shutdown不会真的强制停止线程池
 *
 * 查看 execute() 源码可知，当线程池状态不是Running时，无法添加任务到队列，会执行拒绝策略
 */
public class ShutDownThreadPool {

    public static void main(String[] args) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        Runnable task = new Task();

        for (int i = 0; i < 200; i++) {
            executorService.execute(task);
        }

        Thread.sleep(2000);
        // 通知停止线程
        executorService.shutdown();

        // 判断线程池是否调用了shutdown方法
        if (executorService.isShutdown()) {
            System.out.println("线程池已经停止，不应该继续添加任务");
        }

        // 线程池停止后再添加任务会抛出异常 RejectedExecutionException
        executorService.execute(task);
    }
}
