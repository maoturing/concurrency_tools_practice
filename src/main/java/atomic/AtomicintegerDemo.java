package atomic;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 对比原子类与基础类型，原子类型能不加锁保证线程安全
 *
 * @author mao  2020/3/28 7:13
 */
public class AtomicintegerDemo {
    // 对比项，普通变量无法保证原子性
    public volatile int num = 0;
    private AtomicInteger atomicInteger = new AtomicInteger(0);


    public static void main(String[] args) throws InterruptedException {
        AtomicintegerDemo dmeo = new AtomicintegerDemo();
        // 两个线程各执行1000次累加，期望结果是得到2000
        Thread t1 = new Thread(() -> dmeo.increment());
        Thread t2 = new Thread(() -> dmeo.increment());
        t1.start();
        t2.start();
        // 等待两个线程执行结束
        t1.join();
        t2.join();

        // 打印普通变量的累加结果
        System.out.println(dmeo.num);
        // 打印原子变量的累加结果
        System.out.println(dmeo.atomicInteger.get());
    }

    // 基础类型+1
    public void incrementBasic() {
        num++;
    }

    // 原子类型+1
    public void incrementAtomic() {
        atomicInteger.getAndIncrement();
    }

    // 累加1000次
    public void increment() {
        for (int i = 0; i < 1000; i++) {
            incrementBasic();
            incrementAtomic();
        }
    }
}
