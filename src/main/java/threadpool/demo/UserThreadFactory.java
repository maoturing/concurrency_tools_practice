package threadpool.demo;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 创建线程池, Executors默认线程工厂过于简单,对用户不友好,我们自定义线程工厂
 * <p>
 * 《码出高效》p239
 *
 * @author mao  2019/12/27 4:50
 */
public class UserThreadFactory implements ThreadFactory {
    private final String namePrefix;
    private final AtomicInteger nextId = new AtomicInteger(1);

    // 创建线程工厂时可以设置有意义的名称，方便调试
    UserThreadFactory(String whatFeatrueOfGroup) {
        namePrefix = "UserThreadFactory's " + whatFeatrueOfGroup + "-Worker-";
    }

    @Override
    public Thread newThread(Runnable task) {
        String name = namePrefix + nextId.getAndIncrement();
        Thread thread = new Thread(null, task, name, 0);
        System.out.println(thread.getName());
        // 此处可以设置守护线程，优先级等
        return thread;
    }
}

class Task implements Runnable {
    private final AtomicInteger count = new AtomicInteger(0);
    @Override
    public void run() {
        System.out.println("running_" + count.getAndIncrement());
    }
}

