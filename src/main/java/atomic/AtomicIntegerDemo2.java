package atomic;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 测试使用AtomicInteger常见方法
 *
 * @author mao  2020/3/28 7:30
 */
public class AtomicIntegerDemo2 {
    // 定义全局变量atomicInteger，如果该变量定义到方法内部，线程私有就不需要使用原子类了
    private AtomicInteger atomicInteger = new AtomicInteger(123);

    /**
     * 设置值，不需要使用CAS，
     */
    @Test
    public void testSet() {
        atomicInteger.set(333);
        System.out.println(atomicInteger.get());
    }

    /**
     * 获取值，不需要使用CAS
     */
    @Test
    public void testGet() {
        int result = atomicInteger.get();
        System.out.println(result);
    }

    /**
     * 获取当前值并修改，需要使用CAS来保证获取和修改之间没有其他线程来修改当前值。
     */
    @Test
    public void testGetAndSet() {
        int result = atomicInteger.getAndSet(666);
        // 返回设置之前的值
        System.out.println(result);

        // 返回设置的新值
        System.out.println(atomicInteger.get());
    }

    @Test
    public void testGetAndIncrement() {
        int result = atomicInteger.getAndIncrement();
        // 返回+1之前的值
        System.out.println(result);

        // 返回+1后新值
        System.out.println(atomicInteger.get());
    }

    @Test
    public void testIncrementAndGet() {
        // 返回+1之前的值
        System.out.println(atomicInteger.get());
        int result = atomicInteger.incrementAndGet();
        // 返回+1后新值
        System.out.println(result);
    }

    @Test
    public void testGetAndDecrement() {
        int result = atomicInteger.getAndDecrement();
        // 返回-1之前的值
        System.out.println(result);

        // 返回-1后新值
        System.out.println(atomicInteger.get());
    }

    @Test
    public void testGetAndAdd() {
        int result = atomicInteger.getAndAdd(666);
        // 返回相加前的值
        System.out.println(result);

        // 返回相加后新值
        System.out.println(atomicInteger.get());
    }

    @Test
    public void testCompareAndSet() {
        // 打印当前值
        System.out.println(atomicInteger.get());

        // 使用CAS操作修改当前值，如果当前值为666，则修改为999并返回true
        boolean result = atomicInteger.compareAndSet(666, 999);
        // 是否修改成功，因为初始值不是666
        System.out.println(result);
        System.out.println(atomicInteger.get());

        // 使用CAS操作修改当前值，如果当前值为123，则修改为777，并返回true
        boolean result2 = atomicInteger.compareAndSet(123, 777);
        System.out.println(result);
        System.out.println(atomicInteger.get());
    }

    @Test
    public void testCAS() throws InterruptedException {
        // 打印当前值
        System.out.println(atomicInteger.get());

        // 启动一个线程执行CAS操作来修改atomicInteger的值
        new Thread(() -> cas()).start();

        // 启动一个线程修改atomicInteger的值为666   
        new Thread(() -> atomicInteger.set(666)).start();
        Thread.sleep(2000);

        // 查看使用循环CAS修改成功后的值
        System.out.println(atomicInteger.get());
    }

    private boolean cas() {
        boolean result = false;
        while (!result) {
            // 使用CAS操作修改当前值，如果当前值为666，则修改为999并返回true
            result = atomicInteger.compareAndSet(666, 999);
        }
        return result;
    }
}
