package control.exchanger;

import java.sql.Timestamp;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;

/**
 * @author mao  2021/4/1 6:47
 */
public class ExchangerDemo {
    public static void main(String[] args) {
        Exchanger<String> exchanger = new Exchanger<>();

        new Thread(() -> {

            try {
                // 模拟生产数据
                TimeUnit.SECONDS.sleep(2);
                String dataA = "银行流水A";
                System.out.println("生产数据完成:" + dataA + ", 开始交换传输");

                // 等待另一个线程传输交换数据
                String dataB = exchanger.exchange(dataA);
                System.out.println("收到交换数据:" + dataB);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                // 模拟生产数据
                TimeUnit.SECONDS.sleep(5);
                String dataB = "银行流水B";
                System.out.println("生产数据完成:" + dataB + ", 开始交换传输");

                // 等待另一个线程传输交换数据
                String dataA = exchanger.exchange(dataB);
                System.out.println("收到交换数据:" + dataA);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
