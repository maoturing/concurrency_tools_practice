package atomic;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class AtomicArrayDemo {
    // 对比项，普通数组无法保证原子性
    public ArrayList<Integer> list = new ArrayList(1000);

    private AtomicIntegerArray atomicArray = new AtomicIntegerArray(1000);


    public static void main(String[] args) throws InterruptedException {

        AtomicArrayDemo demo = new AtomicArrayDemo();
        // 初始化普通数组
        for (int i = 0; i < 1000; i++) {
            demo.list.add(i, 0);
        }

        // 100个线程执行+1和减1操作,预期每个元素最终为0
        Thread[] incrementers = new Thread[100];
        Thread[] decrementers = new Thread[100];
        for (int i = 0; i < 100; i++) {
            incrementers[i] = new Thread(() -> demo.increment());
            decrementers[i] = new Thread(() -> demo.decrement());
            incrementers[i].start();
            decrementers[i].start();
        }

        for (int i = 0; i < 100; i++) {
            // 等待线程执行结束
            incrementers[i].join();
            decrementers[i].join();
        }


        System.out.println("下面是原子数组，预期全部是0：");
        // 打印原子数组元素变量
        for (int i = 0; i < demo.atomicArray.length(); i++) {
            System.out.print(demo.atomicArray.get(i) + "，");

        }
        System.out.println();
        System.out.println("下面是普通数组，预期全部是0：");
        for (int i = 0; i < demo.list.size(); i++) {
            System.out.print(demo.list.get(i) + "，");

        }
    }

    // 对数组每个元素都减1
    public void decrement() {
        for (int i = 0; i < atomicArray.length(); i++) {
            // 对原子数组第i个元素减1
            atomicArray.getAndDecrement(i);
            // 对普通数组第i个元素减1
            list.set(i, list.get(i) - 1);
        }
    }

    // 对数组每个元素都加1
    public void increment() {
        for (int i = 0; i < atomicArray.length(); i++) {
            // 对原子数组第i个元素加1
            atomicArray.getAndIncrement(i);
            // 对普通数组第i个元素加1
            list.set(i, list.get(i) + 1);
        }
    }
}
