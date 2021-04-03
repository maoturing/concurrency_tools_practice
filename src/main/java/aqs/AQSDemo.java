package aqs;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author mao  2021/3/28 12:33
 */
public class AQSDemo {

    public static void main(String[] args) {
        ReentrantLock lock = new ReentrantLock(true);

         // ======== 三个用户 A B C 来办理业务, 同时只能有一个用户办理业务 ==========//
        new Thread(() -> {
            lock.lock();
            try {
                System.out.println(" A 用户进来了... ");
                // 休眠10s, 模拟业务执行
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }, "A").start();

        // 第2个客户B进来办理业务, 由于第1个客户还持有锁在办理, 此时B只能等待
        new Thread(() -> {
            lock.lock();
            try {
                System.out.println(" B 用户进来了... ");
                // 休眠10s, 模拟业务执行
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }, "B").start();

        // 第3个客户C进来办理业务, 由于第1个客户还持有锁在办理, 此时C只能等待
        new Thread(() -> {
            lock.lock();
            try {
                System.out.println(" C 用户进来了... ");
                // 休眠10s, 模拟业务执行
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }, "C").start();
    }
}
