package collections.old;

import java.util.Hashtable;

/**
 * Hashtable 是线程安全的HashMap，使用synchronized实现，效率较低
 */
public class HashtableDemo {

    public static void main(String[] args) {
        Hashtable<Integer, String> hashtable = new Hashtable<>();
        hashtable.put(1, "Spring");
        hashtable.put(2, "SpringCloud");
        hashtable.put(3, "Dubbo");

        System.out.println(hashtable.get(2));
    }
}
