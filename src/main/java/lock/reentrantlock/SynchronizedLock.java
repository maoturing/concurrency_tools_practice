package lock.reentrantlock;

/**
 * 演示synchronized的可重入性质
 * 可重入锁的原理是在锁内部维护一个线程表示，来表示该锁目前被哪个线程占用，
 * 然后关联一个计数器，当一个线程获取了该锁，计数器从0变为1，其他线程访问时发现锁的所有者不是自己，则被阻塞
 * 但是当获取了该锁的线程再次获取时会让计数器再+1
 */
public class SynchronizedLock {

    public static void main(String[] args) {
        new SynchronizedLock().hello1();
    }

    /**
     * 该方法加了对象锁
     */
    public synchronized void hello1() {
        System.out.println("hello 1");
        // 当前线程已经获取了对象锁，如果能调用下面的方法，则证明synchronized是可重入锁
        // 如果是不可重入锁，则会一直阻塞在这一步
        hello2();
    }
    /**
     * 该方法加了对象锁
     */
    public synchronized void hello2() {
        System.out.println("hello 2");
    }
}
