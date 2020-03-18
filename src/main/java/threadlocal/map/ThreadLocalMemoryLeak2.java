package threadlocal.map;


import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 线程池中使用ThreadLocal发生内存泄漏
 *
 * 线程池中任务执行完了，由于没有调用线程池shutdown方法，线程池中的核心线程会一直存在，JVM进程也不会退出
 * 下面代码是50个任务，有50个LocalVariable大对象，5个核心线程最后一次调用threadLocal.set(new LocalVariable())，
 * 会一直保存在该线程的ThreadLocalMap属性中，所以最后总共有5个LocalVariable大对象没有被回收
 *
 * 使用Jconsle监控堆内存，发现注释remove，最终占用内存71MB，取消注释，最终占用内存31MB，
 * 差的40MB正好是5个LocalVariable大对象，每个LocalVariable是一个8MB的long数组
 * 注意不要开启debug模式，测的内存可能会有偏差，debug用来查看弱引用是否被回收
 *
 * 问题：线程池中5个线程一直不结束，一直持有ThreadLocalMap，存在对value的强引用，所以出现内存泄漏。
 * 经过我debug发现，ThreadLocalMap中的key即ThreadLocal对象仍然存在来自静态变量threadLocal 的强引用。、
 * 既然key不为null，能访问到value，何来内存泄漏一说？
 *
 * 参考：Java并发编程之美p339
 */
public class ThreadLocalMemoryLeak2 {

    public static final ExecutorService threadPool = Executors.newFixedThreadPool(5);
    public static final ThreadLocal<LocalVariable> threadLocal = new ThreadLocal<>();
    static Set<Thread> threads = new HashSet<>();

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            threadPool.execute(() -> {
                // 使用大对象
                threadLocal.set(new LocalVariable());
                System.out.println("use local variable");

                // 保存线程池中核心线程的引用，用于debug查看ThreadLocalMap中是否保存了对象
                threads.add(Thread.currentThread());

                // 使用完后删除，不执行会造成内存泄漏
//                 threadLocal.remove();
            });
            Thread.sleep(500);
        }
        // 尝试回收线程中ThradLocalMap保存的ThreadLocal的弱引用，因为存在来自静态变量threadLocal的强引用，并不会被回收。
        System.gc();    // jconsole测内存时不要开启，有影响

        // 由于没有调用线程池的shutdown方法，线程池中的核心线程并不会退出，进而JVM也不会退出
        // debug点1，查看threads集合中0.threadLocals.Entry.referent和value，
        // 每个线程的ThreadLocalMap中都有value为LocalVariable大对象没被回收，但是key为ThreadLocal，没有被回收
        // 此时jvm进程并不会退出，因为5个线程还存在，jconsole可以监控堆内存的使用量。
        System.out.println("pool executor over");
    }


    static class LocalVariable {
        // long 是64位8B，数组占用内存则为8MB
        private long[] a = new long[1024 * 1024];
    }
}