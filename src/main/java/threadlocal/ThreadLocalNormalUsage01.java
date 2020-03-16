package threadlocal;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 启动30个线程来执行30个转换时间任务
 *
 */
public class ThreadLocalNormalUsage01 {
    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 30; i++) {
            // 启动一个线程获得初始时间i s后时间
            int finalI = i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // 这里为什么需要final才能传入
                    String date = new ThreadLocalNormalUsage01().toDate(finalI);
                    System.out.println(date);
                }
            }).start();

            Thread.sleep(100);
        }
    }

    public String toDate(int seconds) {
        // 参数单位是毫秒，表示1970.1.1 00:00:00后多少毫秒
        Date date = new Date(1000 * seconds);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        // 输出的是1970-01-01 08:00:10，因为是东八区，需要加8小时
        return dateFormat.format(date);
    }
}
