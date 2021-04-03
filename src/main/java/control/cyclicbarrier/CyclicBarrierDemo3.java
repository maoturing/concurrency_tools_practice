package control.cyclicbarrier;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * @author mao  2021/4/1 6:42
 */
public class CyclicBarrierDemo3 {
    public static void main(String[] args) {
        CyclicBarrier cyclicBarrier = new CyclicBarrier(2);

        new Thread(() -> {
            try {
                System.out.println("线程 t1 开始等待");
                cyclicBarrier.await();
                System.out.println("线程 t1 恢复执行");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                System.out.println("线程 t2 开始等待");
                cyclicBarrier.await();
                System.out.println("线程 t2 恢复执行");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
