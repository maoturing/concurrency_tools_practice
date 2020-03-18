package threadlocal;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 利用ThreadLocal，给每个线程分配自己的SimpleDateFormat对象，
 * 既保证了线程安全，又高效的利用了内存
 *
 * 问题：既然每个线程一份SimpleDateFormat对象，那和02使用局部变量每次创建新对象有什么区别？
 * 02中1000个时间转换任务都需要创建和销毁SimpleDateFormat对象，
 * 而下面是每个线程1份SimpleDateFormat对象，线程池共10个线程，所以共10个SimpleDateFormat对象
 * 节省了内存，避免了对象频繁创建于销毁
 *
 */
public class ThreadLocalNormalUsage05 {
    // 创建10线程的固定线程池
    public static ExecutorService threadPool = Executors.newFixedThreadPool(10);

    // 将SimpleDateFormat对象保存在ThreadLocal中，每个线程一个副本
    public static ThreadLocal<SimpleDateFormat> dateFormatThreadLocal = new ThreadLocal<SimpleDateFormat>(){
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        }
    };

    // 与上面代码等价，lambda写法
//    public static ThreadLocal<SimpleDateFormat> dateFormatThreadLocal = ThreadLocal.withInitial(()->new SimpleDateFormat("yyyy-MM-dd hh:mm:ss"));

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 1000; i++) {
            int finalI = i;

            // 提交转换时间任务到线程池，submit本质还是调用execute
            threadPool.submit(new Runnable() {
                @Override
                public void run() {
                    // 这里为什么需要final才能传入
                    String date = new ThreadLocalNormalUsage05().toDate(finalI);
                    System.out.println(date);
                }
            });
        }

        // 切记关闭线程池
        threadPool.shutdown();
    }

    public String toDate(int seconds) {
        // 参数单位是毫秒，表示1970.1.1 00:00:00后多少毫秒
        Date date = new Date(1000 * seconds);
        Thread t = Thread.currentThread();
        // get 获取当前线程的对象副本
        SimpleDateFormat dataFromat = dateFormatThreadLocal.get();
        return dataFromat.format(date);
    }
}
