package threadpool.demo;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池中用户自定义拒绝策略
 * 策略时打印日志并拒绝任务。
 *
 * 拒绝策略一般使用默认的策略即可，一般不用自定义拒绝策略
 */
public class UserRejectHandler implements RejectedExecutionHandler {
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        System.out.println("you task is rejected. " + executor.toString());
    }
}
