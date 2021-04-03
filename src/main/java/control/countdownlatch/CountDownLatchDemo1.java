package control.countdownlatch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 需求：工厂中质检环节, 有5个质检员检查, 所有人都认为通过, 才通过
 *
 * 1. 主线程进行等待，等待 5 个质检员线程质检`latch.await()`
 * 2. 质检员线程开始质检，检查完成, 计数器减 1`latch.countDown()`
 * 3. 等待所有质检员质检结束，主线程恢复运行发布质检结果
 *
 * @author mao  2021/3/31 8:01
 */
public class CountDownLatchDemo1 {
    public static void main(String[] args) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(5);
        ExecutorService executorService = Executors.newFixedThreadPool(5);

        for (int i = 0; i < 5; i++) {
            final int num = i + 1;
            Runnable task = () -> {
                check(latch, num);
            };
            // 提交任务到线程池执行
            executorService.execute(task);
        }
        System.out.println("等待5个质检员检查....");

        // 1.主线程进行等待, 等待5个线程质检结束
        latch.await();

        // 3. 发布质检结果
        System.out.println("5 个质检员检查结束, 产品通过");
        executorService.shutdown();
    }

    // 模拟质检员检查产品
    private static void check(CountDownLatch latch, int num) {
        try {
            // 模拟检查
            TimeUnit.SECONDS.sleep((long) (Math.random()*5));

            System.out.println("No." + num + "完成了检查");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // 2.检查完成, 计数器减1
            latch.countDown();
        }
    }
}