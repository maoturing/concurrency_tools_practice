package threadlocal;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 启动两个线程来转换时间
 *
 */
public class ThreadLocalNormalUsage00 {
    public static void main(String[] args) {
        // 启动一个线程获得初始时间10s后时间
        new Thread(new Runnable() {
            @Override
            public void run() {

                String date = new ThreadLocalNormalUsage00().toDate(10);
                System.out.println(date);
            }
        }).start();

        // 启动一个线程获得初始时间10s后时间
        new Thread(new Runnable() {
            @Override
            public void run() {
                String date = new ThreadLocalNormalUsage00().toDate(1007);
                System.out.println(date);
            }
        }).start();

    }

    public String toDate(int seconds) {
        // 参数单位是毫秒，表示1970.1.1 00:00:00后多少毫秒
        Date date = new Date(1000 * seconds);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        // 输出的是1970-01-01 08:00:10，因为是东八区，需要加8小时
        return dateFormat.format(date);
    }
}
