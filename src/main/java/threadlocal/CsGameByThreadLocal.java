package threadlocal;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 存在问题！
 *
 * 使用ThreadLocal实现cs游戏
 * <p>
 * 游戏开始时，每个玩家能领导一把枪，拥有三个数：子弹数，杀敌数，生命值，为其初始值为1500,0，100
 *
 *
 * 参考：码出高效ThreadLocal章节
 */
public class CsGameByThreadLocal {
    public static final ThreadLocalRandom random = ThreadLocalRandom.current();

    public static final Integer TOTAL_PLAYER = 10;

    private static final ThreadLocal<Integer> bulletNumber = ThreadLocal.withInitial(() -> 1500);
    private static final ThreadLocal<Integer> killedEnemites = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Integer> leftValue = ThreadLocal.withInitial(() -> 100);

    public static void main(String[] args) {
        for (int i = 0; i < TOTAL_PLAYER; i++) {
//            new Thread(new Player("player"+i)).start();
            new Player("player"+i).start();
        }
    }

    static class Player extends Thread {
        private String name;

        Player(String name) {
            this.name = name;
        }
        @Override
        public void run() {

            Integer bullets = bulletNumber.get() - random.nextInt(1500);
            Integer kills = killedEnemites.get() + random.nextInt(TOTAL_PLAYER / 2);
            Integer left = leftValue.get() - random.nextInt(100);
            String playerNme = Thread.currentThread().getName();
            System.out.println(playerNme + "剩余子弹数:" + bullets);
            System.out.println(playerNme + "杀敌数:" + kills);
            System.out.println(playerNme + "生命值:" + left);
            System.out.println();
        }
    }
}
