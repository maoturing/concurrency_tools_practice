package threadlocal;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 使用线程池来执行1000个转换时间任务，如果按照01，启动1000个线程成本非常大
 *
 * 控制台可以看到1000行输出
 */
public class ThreadLocalNormalUsage02 {
    // 创建10线程的固定线程池
    public static ExecutorService threadPool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 1000; i++) {
            int finalI = i;

            // 提交转换时间任务到线程池，submit本质还是调用execute
            threadPool.submit(new Runnable() {
                @Override
                public void run() {
                    // 这里为什么需要final才能传入
                    String date = new ThreadLocalNormalUsage02().toDate(finalI);
                    System.out.println(date);
                }
            });
        }

        // 关闭线程池
        threadPool.shutdown();
    }

    public String toDate(int seconds) {
        // 参数单位是毫秒，表示1970.1.1 00:00:00后多少毫秒
        Date date = new Date(1000 * seconds);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        // 输出的是1970-01-01 08:00:10，因为是东八区，需要加8小时
        return dateFormat.format(date);
    }
}
