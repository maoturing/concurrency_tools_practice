package control.condition;

import java.util.PriorityQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author mao  2021/3/31 19:43
 */
public class ConditionDemo2 {
    private int queueSiez = 10;
    private PriorityQueue<Integer> queue = new PriorityQueue<>(queueSiez);
    private Lock lock = new ReentrantLock();
    // 队列不满, 则可以生产
    private Condition notFull = lock.newCondition();
    // 队列不空, 则可以消费
    private Condition notEmpty = lock.newCondition();

    public static void main(String[] args) {
        ConditionDemo2 demo2 = new ConditionDemo2();
        Producer producer = demo2.new Producer();
        Consumer consumer = demo2.new Consumer();

        producer.start();
        consumer.start();
    }

    class Producer extends Thread {
        @Override
        public void run() {
            while (true) {
                produce();
            }
        }

        private void produce() {
            try {
                lock.lock();
                // 队列满, 生产线程等待
                // 使用while是为了避免虚假唤醒
                while (queue.size() == queueSiez) {
                    System.out.println("队列已满, 唤醒消费者消费数据");
                    notFull.await();
                }
                // 生产一个元素
                int ele = (int) (Math.random() * 10);
                queue.add(ele);
                System.out.println("生产数据 num: " + ele + ", 队列元素数量: " + queue.size());

                // 队列不空, 唤醒所有生产线程
                notEmpty.signalAll();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }
    }

    class Consumer extends Thread {

        @Override
        public void run() {
            while (true) {
                consume();
            }
        }

        private void consume() {
            try {
                lock.lock();
                // 队列空, 消费线程等待
                // 使用while是为了避免虚假唤醒
                while (queue.size() == 0) {
                    System.out.println("队列为空, 等待生产者生产数据");
                    notEmpty.await();
                }
                // 消费一个元素
                Integer ele = queue.poll();
                System.out.println("消费元素 ele: " + ele + ", 队列剩余元素: " + queue.size());

                // 队列不满, 唤醒所有生产线程
                notFull.signalAll();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }
    }
}

