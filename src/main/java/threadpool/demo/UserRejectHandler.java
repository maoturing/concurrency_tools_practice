package threadpool.demo;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池中用户自定义拒绝策略
 * 策略时打印日志并拒绝任务。
 * <p>
 * 拒绝策略一般使用默认的策略即可，一般不用自定义拒绝策略
 */
public class UserRejectHandler implements RejectedExecutionHandler {
    // 统计任务拒绝次数
    private final AtomicInteger rejectCount = new AtomicInteger(1);

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        System.out.println("you task is rejected. count=" + rejectCount.getAndIncrement() + ". " + executor.toString());
    }
}
