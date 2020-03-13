package threadpool.stop;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 判断线程池是否已经停止，使用isTerminated()方法
 * 与shutdown不同，isTerminated判断是线程池是否真的停止了
 * 解决了shutdown无法得知线程是否真的停止的问题
 */
public class TerminatedThreadPool {

    public static void main(String[] args) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        Runnable task = new Task();

        for (int i = 0; i < 100; i++) {
            executorService.execute(task);
        }

        Thread.sleep(2000);
        executorService.shutdown();

        // 判断线程池是否调用了shutdown方法
        if (executorService.isShutdown()) {
            System.out.println("线程池已经停止，不应该继续添加任务");
        }
        // 判断线程池是否真的停止，任务未执行完，返回false
        System.out.println("线程池是否已经停止" + executorService.isTerminated());
        // 等待10s任务执行完，返回true
        Thread.sleep(10000);
        System.out.println("线程池是否已经停止" + executorService.isTerminated());

    }
}

class Task implements Runnable {
    @Override
    public void run() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 输出执行任务的线程名称
        System.out.println(Thread.currentThread().getName());
    }
}