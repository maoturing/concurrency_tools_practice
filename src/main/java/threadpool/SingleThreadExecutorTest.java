package threadpool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SingleThreadExecutor 示例，创建只有一个线程的线程池
 *
 * Executors.newSingleThreadExecutor() 是构建了一个core 和 maxSize都为1的线程池。
 *
 * 缺点是当大量请求堆积时，会占用大量内存
 */
public class SingleThreadExecutorTest {
    public static void main(String[] args) {
        ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
        for (int i = 0; i < 100; i++) {
            // Task是输出线程名称，发现永远是同一个线程只执行任务
            singleThreadExecutor.execute(new Task());
        }
    }
}
