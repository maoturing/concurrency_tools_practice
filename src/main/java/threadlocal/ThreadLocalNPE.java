package threadlocal;

/**
 * 描述： ThreadLocal 出现空指针异常
 *
 * 如果ThreadLocal没有先set直接get会返回null，而不会抛出异常。为什么下面代码出现了空指针异常？
 * 拆箱操作导致空指针异常，解决办法是将getValue返回值修改为Long
 *
 */
public class ThreadLocalNPE {

    ThreadLocal<Long> longThreadLocal = new ThreadLocal<>();

    // get() 方法返回值是Long类型，拆箱转换为long类型是Long.longValue()，如果返回值为null则会发现空指针异常
    // 解决办法：将getValue返回值修改为Long
    public long getValue() {
        // 可能出现空指针异常
        return longThreadLocal.get();
    }

    public static void main(String[] args) {
        ThreadLocalNPE threadLocal = new ThreadLocalNPE();
        System.out.println(threadLocal.getValue());
    }
}
