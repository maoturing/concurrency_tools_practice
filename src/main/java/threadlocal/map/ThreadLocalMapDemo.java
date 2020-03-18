package threadlocal.map;

import java.text.SimpleDateFormat;

/**
 * 每个Thread 对象都有一个ThreadLocalMap来保存所有ThreadLocal变量
 *
 * 下面代码线程中ThreadLocalMap保存了两个ThreadLocal变量
 * 线程0中两个变量：[k=s1,v="Thread-0-s1"]  [k=s2,v="Thread-0-s2"]
 * 线程1中两个变量：[k=s1,v="Thread-1-s1"]  [k=s2,v="Thread-1-s2]
 * 说明："Thread-1-s2"，表示线程1中对象s2的副本
 *
 * 使用ThreadLocal不会出现线程1输出o1.get()得到Thread-1-o2的线程不安全问题
 */
public class ThreadLocalMapDemo {
    public static ThreadLocal<String> s1 = new ThreadLocal<>();
    public static ThreadLocal<String> s2 = new ThreadLocal<>();

    public static void main(String[] args) {
        // 线程0对两个对象设置值，并输出值
        new Thread(() -> {
            s1.set(Thread.currentThread().getName() + "-s1");
            System.out.println(s1.get());

            s2.set(Thread.currentThread().getName() + "-s2");
            System.out.println(s2.get());
        }).start();

        // 线程1对两个对象设置值，并输出值
        new Thread(() -> {
            s1.set(Thread.currentThread().getName() + "-s1");
            System.out.println(s1.get());

            s2.set(Thread.currentThread().getName() + "-s2");
            System.out.println(s2.get());
        }).start();
    }
}
