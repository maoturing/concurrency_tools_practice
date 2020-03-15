package threadpool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class ThreadPoolState {
    public static void main(String[] args) {
        System.out.println(Integer.toBinaryString(-1));
        Integer a = -1 << 2;
        System.out.println(Integer.toBinaryString(a));

        System.out.println(Integer.toBinaryString(-1 << 29));
        System.out.println(Integer.toBinaryString(0 << 29));
    }
}



    /**
     * 下面是使用示例
     **/
    // 创建有界阻塞队列
