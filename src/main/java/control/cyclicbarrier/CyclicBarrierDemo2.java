package control.cyclicbarrier;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

/**
 * 同样还是旅游车, 每到齐5个乘客后就开始发车, 即可以循环使用
 * @author mao  2021/4/1 4:55
 */
public class CyclicBarrierDemo2 {
    public static void main(String[] args) {
        // 这里Runnable是屏障打开之后执行的
        CyclicBarrier cyclicBarrier = new CyclicBarrier(5, new Runnable() {
            @Override
            public void run() {
                System.out.println("5人车坐满了, 发车了...");
            }
        });

        // 启动10个线程
        for (int i = 0; i < 10; i++) {
            new Thread(new CyclicBarrierDemo.Task(i, cyclicBarrier)).start();
        }
    }
}
