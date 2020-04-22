package atomic;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 描述：     演示高并发场景下，LongAdder比AtomicLong性能好
 */
public class LongAdderDemo {

    public static void main(String[] args) throws InterruptedException {
        // 计数器
        LongAdder counter = new LongAdder();
        ExecutorService service = Executors.newFixedThreadPool(20);
        long start = System.currentTimeMillis();

        // 1w个累加任务，每个累加任务执行1w次累加操作
        for (int i = 0; i < 10000; i++) {
            service.submit(new Task(counter));
        }
        service.shutdown();
        // 等待任务执行完毕
        while (!service.isTerminated()) {

        }

        long end = System.currentTimeMillis();

        // 打印计数器结果和耗时
        System.out.println(counter.sum());
        System.out.println("LongAdder耗时：" + (end - start));
    }

    private static class Task implements Runnable {

        private LongAdder counter;

        public Task(LongAdder counter) {
            this.counter = counter;
        }

        // 累加任务，任务内容是累加1w次
        @Override
        public void run() {
            for (int i = 0; i < 10000; i++) {
                counter.increment();
            }
        }
    }
}
