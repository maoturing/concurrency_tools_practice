package threadpool;

/**
 * 每个任务都创建一个新的线程来执行
 *
 *  @author mao  2020-3-8 18:49:05
 */
public class EveryTaskOneThread {
    public static void main(String[] args) {
        Runnable task = () -> System.out.println("执行了任务...");

        // 1. 执行一个任务
        Thread thread = new Thread(task);
        thread.start();

        // 2. 执行1000个任务
        for (int i = 0; i < 1000; i++) {
            Thread thread1 = new Thread(task);
            thread1.start();
        }
    }
}
