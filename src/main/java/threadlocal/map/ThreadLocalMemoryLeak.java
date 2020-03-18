package threadlocal.map;

/**
 * 演示ThreadLocal内存泄漏
 *
 * 循环中对local重复赋值, 导致当前线程中的threadLocals(ThreadLocalMap)中的Entry的key的失去强引用,只剩下Entry的弱引用
 * GC后,可以观察到Entry的key referent为null,弱引用已经被回收.
 * 最后只有value为"text-4"的Entry中的key不为null,因为ThreadLocal local存在对其的强引用
 */
public class ThreadLocalMemoryLeak {

    public static void main(String[] args) {
        // 这样创建ThreadLocal，循环中重复设置set值不会发生内存泄漏，因为是同一个key，修改value而已
        // ThreadLocal<String> local = new ThreadLocal<>();


        ThreadLocal<String> local = null;
        for (int i = 0; i < 5; i++) {
            // 1. 重复创建ThreadLocal，上一次循环创建的ThreadLocal会失去强引用，
            // 是造成内存泄漏的源头
            local = new ThreadLocal<>();
            local.set("text-" + i);

            // 发生gc后会清除弱引用，get会清除ThreadLocalMap中key==null的Entry
            // 因为没有GC，所以ThreadLocalMap中Entry的key都不为null
            System.out.println(local.get());

            // 使用完后移除ThreadLocal，防止内存泄漏
            // local.remove();
        }
        // 获取当前线程，debug查看ThreadLocalMap中的Entry
        Thread thread = Thread.currentThread();

        // debug点1，查看thread.threadLocals.Entry.referent
        // 弱引用对象在gc时被回收，这一步之前，thread.ThreadLocalMap中所有Entry的key(referent)都不为null
        System.gc();

        // debug点2，查看thread.threadLocals属性
        // ThreadLocalMap中前四个Entry key(referent)都为null，最后一次循环[local,text-4]中local存在强引用，不会被回收
        System.out.println();

        // debug点3，前面进行了GC，再次调用get会删除ThreadLocalMap中key==null的Entry
        // 经过debug发现，并没有删除ThreadLocalMap中key==null的Entry，为什么？
        String text = local.get();

        System.out.println(text);
    }
}