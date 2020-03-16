package threadlocal;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 问题：02有一个缺点，toDate方法中每次都会创建新的SimpleDateFormat对象，
 * 1000个时间转换任务就会创建1000个对象，对象创建和GC开销大
 *
 * 解决办法：我们可以把SimpleDateFormat对象作为全局变量，只创建一个，大家一起使用，节省了内存
 *
 * 存在问题：以上好像解决了内存开销大的问题，但是会引入线程安全问题，在输出结果中，存在相同的时间
 * 这是因为所有线程共用一个SimpleDateFormat对象时，线程切换就会导致出现相同时间
 *
 * SimpleDateFormat线程不安全原因如下：
 * https://blog.csdn.net/weixin_38810239/article/details/79941964
 */
public class ThreadLocalNormalUsage03 {
    // 创建10线程的固定线程池
    public static ExecutorService threadPool = Executors.newFixedThreadPool(10);
    // 作为全局变量(类属性)，避免多次创建，节省内存
    public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 1000; i++) {
            int finalI = i;

            // 提交转换时间任务到线程池，submit本质还是调用execute
            threadPool.submit(new Runnable() {
                @Override
                public void run() {
                    // 这里为什么需要final才能传入
                    String date = new ThreadLocalNormalUsage03().toDate(finalI);
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

        // 输出的是1970-01-01 08:00:10，因为是东八区，需要加8小时
        return dateFormat.format(date);
    }
}
