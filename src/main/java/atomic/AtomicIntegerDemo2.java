package atomic;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 测试使用AtomicInteger常见方法
 * @author mao  2020/3/28 7:30
 */
public class AtomicIntegerDemo2 {
    // 定义全局变量atomicInteger，如果该变量定义到方法内部，线程私有就不需要使用原子类了
    private AtomicInteger atomicInteger = new AtomicInteger(123);

    @Test
    public void testSet() {
        atomicInteger.set(333);
        System.out.println(atomicInteger.get());
    }

    @Test
    public void testGet() {
        int result = atomicInteger.get();
        System.out.println(result);
    }

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

}
