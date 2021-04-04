package control.countdownlatch;

import java.sql.Timestamp;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 *  需求：百米赛跑，5 名运动员线程准备好后进行等待，当裁判员线程发令枪响，所有运动员线程才可以开始跑步
 *
 * 1. 5个运动员线程准备好后进行等待`latch.await()`
 * 2. 裁判员线程发令枪响`latch.countDown()`
 * 3. 5个运动员线程开始跑步
 *
 * @author mao  2021/3/31 8:23
 */
public class CountDownLatchDemo2 {
    public static void main(String[] args) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(5);

        for (int i = 0; i < 5; i++) {
            final int num = i + 1;
            Runnable task = () -> {
                prepare(latch, num);
            };
            // 提交任务到线程池执行
            executorService.execute(task);
        }
        // 模拟裁判员准备工作
        TimeUnit.SECONDS.sleep(4);

        System.out.println("发令枪响, 比赛开始!");
        // 2.裁判员线程发令枪响
        // 倒计数为0, 所有运动员可以起跑了
        latch.countDown();
        executorService.shutdown();
    }

    private static void prepare(CountDownLatch latch, int num) {
        System.out.println("No." + num + "运动员准备完毕, 等待发令枪");
        try {
            // 1.运动员线程准备好后进行等待
            latch.await();
            // 3.5个运动员线程开始跑步
            System.out.println("No." + num + "运动员已经起跑");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

