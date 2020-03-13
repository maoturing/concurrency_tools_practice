package threadpool.stop;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 强制停止线程池 shutDownNow
 * 强制停止线程，发送interrupt通知，停止正在执行的线程池，返回任务队列中的任务
 */
public class ShutDownNowThreadPool {
    public static void main(String[] args) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        Runnable task = new Task2();

        for (int i = 0; i < 200; i++) {
            executorService.execute(task);
        }
        Thread.sleep(2000);

        // 强制停止线程，发送interrupt通知，停止正在执行的线程池，返回任务队列中的任务
        List<Runnable> stopTasks = executorService.shutdownNow();
        System.out.println(stopTasks.size() + "个任务被停止，应记录到日志或数据库中");
    }
}

class Task2 implements Runnable {
    @Override
    public void run() {
        try {
            Thread.sleep(200);
            // 输出执行任务的线程名称
            System.out.println(Thread.currentThread().getName());

        } catch (InterruptedException e) {
            System.out.println(Thread.currentThread().getName() + "线程被中断了...");
        }

    }
}
