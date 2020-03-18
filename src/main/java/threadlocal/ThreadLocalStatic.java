package threadlocal;

/**
 * 有问题，没搞明白
 * staticInt是全局变量，任何线程都可以修改，如果把该变量
 */
public class ThreadLocalStatic {
    public static Integer staticInt = new Integer(666);
    public static ThreadLocal<Integer> threadLocal = new ThreadLocal<>();
    public static ThreadLocal<Integer> threadLocal2 = new ThreadLocal<>();

    public static void main(String[] args) {

        new Thread(()-> {

            threadLocal.set(staticInt);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("thread 1 get staticInt: " + threadLocal.get());

            threadLocal2.set(111);
            System.out.println(threadLocal2.get());
        }).start();

        new Thread(()-> {
            staticInt = new Integer(123);
            threadLocal.set(staticInt);
            System.out.println("thread 2 get staticInt: " + threadLocal.get());

            threadLocal2.set(222);
            System.out.println(threadLocal2.get());
        }).start();
    }
}
