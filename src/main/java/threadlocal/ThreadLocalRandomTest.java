package threadlocal;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

public class ThreadLocalRandomTest {
    public static void main(String[] args) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        Runnable task = () -> {
            System.out.println(random.nextInt(5));
            System.out.println(random.nextInt(5));
            System.out.println(random.nextInt(5));

        };
        for (int i = 0; i < 10; i++) {
            executorService.execute(task);
        }

    }
}
