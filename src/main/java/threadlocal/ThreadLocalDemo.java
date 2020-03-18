package threadlocal;

import org.junit.Test;

import java.lang.reflect.Field;

/**
 * Java中的ThreadLocal类允许我们创建只能被同一个线程读写的变量。ThreadLocal中填充的变量属于当前线程，该变量对其他线程而言是隔离的。
 * ThreadLocal将数据保存到ThreadLocalMap中,key为线程,value为set值,
 * <p>
 * 使用场景: SpringMVC中Controller和Service是单例的,全局变量设置为ThreadLocal可以达到线程隔离的作用
 * <p>
 * 参考文档: https://droidyue.com/blog/2016/03/13/learning-threadlocal-in-java/
 * Created by User on 2019/1/7.
 */
public class ThreadLocalDemo {
    /**
     * api的使用: set, get
     */
    @Test
    public void test() throws InterruptedException {
        ThreadLocal<String> local = new ThreadLocal<>();
        System.out.println(local.get());
        local.set("mwq");
        local.set("123");

        System.out.println(local.get());
    }

    @Test
    public void testRemove() throws InterruptedException {
        ThreadLocal<String> local = new ThreadLocal<>();
        System.out.println(local.get());
        local.set("mwq");
        local.remove();

        System.out.println(local.get());
    }

    /**
     * 多个线程使用全局变量local,互不影响
     */
    @Test
    public void testThreadLocal() {
        ThreadLocal<String> local = new ThreadLocal<>();    // 全局变量
        local.set(Thread.currentThread().getName() + " haha");


        new Thread(
                () -> {
                    System.out.println(local.get());
                    local.set(Thread.currentThread().getName() + " world");
                    System.out.println(local.get());    //Thread-1 world
                }
        ).start();
        Thread t = new Thread() {
            @Override
            public void run() {
                System.out.println(local.get());
                local.set(Thread.currentThread().getName() + " hello");
                System.out.println(local.get());       // Thread-0 hello
            }
        };
        t.start();
        System.out.println(local.get());    //main haha
    }

    /**
     * 重写initialValue()方法为ThreadLocal设置初始值
     * 默认为null，在第一次调用get方法时指定重写的initialValue()
     */
    @Test
    public void testInit() {
        ThreadLocal<String> local = new ThreadLocal<String>() {
            @Override
            protected String initialValue() {
                return Thread.currentThread().getName();
            }
        };
        System.out.println(local.get());
    }

    @Test
    public void testInit2() {
        ThreadLocal<String> local = ThreadLocal.withInitial(() -> Thread.currentThread().getName());
        System.out.println(local.get());
    }


    /**
     * 测试ThreadLocal内存泄漏
     * 循环中对local重复赋值, 导致当前线程中的threadLocals(ThreadLocalMap)中的Entry的key的失去强引用,只剩下Entry的弱引用,
     * GC后,可以观察到Entry的key referent为null,已经被回收.
     * 只有value为"aa99"的Entry中的key不为null,因为下面的代码中存在对该ThreadLocal的强引用
     * <p>
     * 什么时候回收key为null的Entry? 博文说会在调用get方法时删除.经过debug,发现无法跳转到getEntryAfterMiss分支
     * 其实并没有搞清楚
     *
     * @throws Exception https://blog.csdn.net/xlgen157387/article/details/78298840
     */
    @Test
    public void testMemLeak() throws Exception {
        ThreadLocal<String> local = null;
        for (int i = 0; i < 10; i++) {
            final int j = i;
            // 正确的使用方法是remove掉再重新赋值,因为remove方法将referent和value都被设置为null,GC就能回收value内存了
            // local.remove();

            // 这一步是重新创建了ThreadLocal对象并设置value,赋值给local,也是造成内存泄漏的源头
//            local = ThreadLocal.withInitial(() -> "aa" + j);
            local = new ThreadLocal<>();
            local.set("aa" + j);
            System.out.println(local.get());
        }
        Thread thread = Thread.currentThread();
        System.gc();
        Field field = Thread.class.getDeclaredField("threadLocals");
        field.setAccessible(true);
        // 获取threadLocals的属性值
        Object map = field.get(Thread.currentThread());
        Class<? extends Object> localMapClz = map.getClass();
        Field table = localMapClz.getDeclaredField("table");
        table.setAccessible(true);
        Object t = table.get(map);  // 可以观察到ThreadLocal$ThreadLocalMap$Entry中referent为null

        // 也可以不使用反射方法查看,可以直接从当前线程Thread.threadLocals.Entry.referent查看ThreadLocal(key)是否被回收
        Thread t2 = Thread.currentThread();
        System.out.println(local.get());
        System.out.println("end");

    }
}
