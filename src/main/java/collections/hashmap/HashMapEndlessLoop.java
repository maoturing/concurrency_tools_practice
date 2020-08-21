package collections.hashmap;

import java.util.HashMap;

public class HashMapEndlessLoop {

    // 设置初始容量和负载因子，方便在扩容时复现CPU100%的问题
    private static HashMap<Integer, String> map = new HashMap<>(2, 1.5f);

    public static void main(String[] args) {
        // 这三个key的hash值%容量，会出现在同一个槽
        map.put(5,"C");
        map.put(7,"B");
        map.put(3,"A");

        new Thread(new Runnable() {
            @Override
            public void run() {
                map.put(15, "D");
                System.out.println(map);
            }
        }, "t1").start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                map.put(1, "E");
                System.out.println(map);
            }
        }, "t2").start();
    }

}
