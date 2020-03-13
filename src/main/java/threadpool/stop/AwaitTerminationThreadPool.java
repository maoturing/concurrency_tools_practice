package threadpool.stop;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 一定时间内检测线程是否停止 awaitTermination
 *
 * 注意：刚方法并不是等到规定时间后，检测线程池状态并返回，而是死循环一直检测线程池状态，
 * 如果已停止则返回true，未停止则一直检测，直至到规定时间并返回false
 */
public class AwaitTerminationThreadPool {
    public static void main(String[] args) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        Runnable task = new Task();

        for (int i = 0; i < 200; i++) {
            executorService.execute(task);
        }

        Thread.sleep(1000);
        // 通知停止线程
        executorService.shutdown();

        // 检测线程池1s内线程池是否停止
        boolean isTerminated = executorService.awaitTermination(1L, TimeUnit.SECONDS);
        System.out.println("等待1s并检测线程池是否停止：" + isTerminated);

        // 检测线程池10s内是否停止
        isTerminated = executorService.awaitTermination(10L, TimeUnit.SECONDS);
        System.out.println("等待5s并检测线程池是否停止：" + isTerminated);
    }
}
