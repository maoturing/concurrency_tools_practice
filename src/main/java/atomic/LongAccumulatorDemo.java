package atomic;

import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

public class LongAccumulatorDemo {
    public static void main(String[] args) {
        // 累加器，初始值为100，传入函数是表示对传入数值和当前值进行的运算
        LongAccumulator longAccumulator = new LongAccumulator((x, y) -> x + y, 100);
        // 传入值为1，根据传入函数，是将1与当前值相加
        longAccumulator.accumulate(1);
        longAccumulator.accumulate(2);

        System.out.println(longAccumulator.get());

        LongAdder adder = new LongAdder();
        adder.increment();

        LongAccumulator accumulator = new LongAccumulator((x, y) -> x + y, 0);
        accumulator.accumulate(1);
    }
}
