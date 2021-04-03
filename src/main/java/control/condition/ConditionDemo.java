package control.condition;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author mao  2021/3/31 19:35
 */
public class ConditionDemo {
    private ReentrantLock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();

    public static void main(String[] args) {
        ConditionDemo conditionDemo = new ConditionDemo();
        new Thread(() -> {
            conditionDemo.conditionWait();
        }, "t1").start();

        new Thread(() -> {
            conditionDemo.conditionSignal();
        }, "t2").start();
    }

    public void conditionWait() {
        lock.lock();
        try {
            System.out.println("条件不满足, 开始等待await...");
            condition.await();
            System.out.println("条件满足, 开始执行后续的任务...");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public void conditionSignal() {
        lock.lock();
        try {
            System.out.println("准备工作完成, 唤醒其他线程signal...");
            condition.signal();
        } finally {
            lock.unlock();
        }
    }
}
