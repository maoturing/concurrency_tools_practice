package threadlocal;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 既然03存在线程安全问题，按照传统解决办法，就是加锁
 *
 * 通过对dateFormat.format(date)语句加锁，每次只能有一个线程
 *
 * 缺点是速度没有02快
 *
 */
public class ThreadLocalNormalUsage04 {
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
                    String date = new ThreadLocalNormalUsage04().toDate(finalI);
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
        String result = null;

        // 加锁，锁是类锁，多个线程不能同时调用format函数
        synchronized (ThreadLocalNormalUsage04.class) {
            // 输出的是1970-01-01 08:00:10，因为是东八区，需要加8小时
            result = dateFormat.format(date);
        }
        return result;
    }
}
