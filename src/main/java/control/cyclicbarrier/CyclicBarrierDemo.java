package control.cyclicbarrier;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

/**
 * 旅游车需要等所有人都到齐之后才能出发
 * 主线程是旅游车, 每个人到达之后会进行等待, 直至所有人到齐
 *
 * @author mao  2021/4/1 4:55
 */
public class CyclicBarrierDemo {
    public static void main(String[] args) {
        // 这里Runnable是5个线程到齐之后执行该方法
        CyclicBarrier cyclicBarrier = new CyclicBarrier(5, new Runnable() {
            @Override
            public void run() {
                System.out.println("所有人都到齐了, 大家统一出发...");
            }
        });

        // 启动5个线程
        for (int i = 0; i < 5; i++) {
            new Thread(new Task(i, cyclicBarrier)).start();
        }
    }

    static class Task implements Runnable {
        private int id;
        private CyclicBarrier cyclicBarrier;

        public Task(int id, CyclicBarrier cyclicBarrier) {
            this.id = id;
            this.cyclicBarrier = cyclicBarrier;
        }

        @Override
        public void run() {
            System.out.println(id + "开始前往集合地点");
            try {
                TimeUnit.SECONDS.sleep((long) (Math.random() * 10));

                System.out.println(id + "到达了集合地点， 开始等待其他人");
                cyclicBarrier.await();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
