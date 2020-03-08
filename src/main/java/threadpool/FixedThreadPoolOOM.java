package threadpool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/**
 * FixedThreadPool 是固定性线程数的线程池，任务队列是无界队列LinkedBlockingQueue
 * 存在OOM 风险，示例代码中，任务执行的速度跟不上任务生成的速度，导致任务队列任务过多
 * 最终出现OutOfMemoryError: GC overhead limit exceeded
 * 这也提醒我们，在使用FixedThreadPool时需要注意OOM风险
 *
 * 执行代码的VM参数 -Xmx8m -Xms8m，缩小堆内存，方便快速复现OOM
 *
 *  @author mao  2020-3-8 18:49:25
 */
public class FixedThreadPoolOOM {
    private static ExecutorService executorService = Executors.newFixedThreadPool(2);

    public static void main(String[] args) {
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            executorService.execute(new Task2());
        }
    }

}

class Task2 implements Runnable {
    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName());
        try {
            Thread.sleep(1000000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
