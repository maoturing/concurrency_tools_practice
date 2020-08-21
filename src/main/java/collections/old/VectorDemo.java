package collections.old;

import java.util.Vector;

/**
 * Vector 是线程安全的ArrayList，add和get方法都是使用synchronized来保证线程安全
 * 在高并发环境下，共用一把锁，效率很低。
 */
public class VectorDemo {
    public static void main(String[] args) {
        Vector<String> vector = new Vector<>();
        vector.add("test");
        System.out.println(vector.get(0));
    }
}
