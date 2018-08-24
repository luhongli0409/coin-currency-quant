package com.cjie.commons.okex.open.api.utils;

import org.apache.commons.lang3.RandomUtils;

import java.math.BigDecimal;
import java.util.Random;

/**
 * @Author: hongli.lu
 * @Date: 2018/8/22 下午2:48
 */

public class HongBaoAlgorithm {

    private static Random random = new Random();

    //小数点位数
    private static int FLOOR_NUM = 8;

    private static int ROUNDING_MODE = BigDecimal.ROUND_HALF_DOWN;

    private static BigDecimal EXPAND_MULTIPLE = new BigDecimal("100000000");

    static {
        random.setSeed(System.currentTimeMillis());
    }

    public static void main(String[] args) {
        long max = new BigDecimal("0.001").multiply(EXPAND_MULTIPLE).longValue();
        long min = new BigDecimal("0.00000001").multiply(EXPAND_MULTIPLE).longValue();
        long amount = new BigDecimal("0.0001").multiply(EXPAND_MULTIPLE).longValue();

        long[] result = generate(amount, 10, max, min);

        BigDecimal totalValue = new BigDecimal("0");
        for (int i = 0; i < result.length; i++) {
            BigDecimal value = new BigDecimal(result[i]).divide(EXPAND_MULTIPLE, FLOOR_NUM, ROUNDING_MODE);
            System.out.println(value.toPlainString());
            totalValue = totalValue.add(value);
        }
        long maxValue = maxValue(result);
        System.out.println("maxValue :" + new BigDecimal(maxValue).divide(EXPAND_MULTIPLE, FLOOR_NUM, ROUNDING_MODE).toPlainString());
        System.out.println("total :" + totalValue.toPlainString());
    }

    /**
     * 生产min和max之间的随机数，但是概率不是平均的，从min到max方向概率逐渐加大。
     * 先平方，然后产生一个平方值范围内的随机数，再开方，这样就产生了一种“膨胀”再“收缩”的效果。
     *
     * @param min
     * @param max
     * @return
     */
    static long xRandom(long min, long max) {
        long randdomValue = (max - min) == 0 ? min : max - min;
        return sqrt(nextLong(sqr(randdomValue)));
    }


    /**
     * @param total 红包总额
     * @param count 红包个数
     * @param max   每个小红包的最大额
     * @param min   每个小红包的最小额
     * @return 存放生成的每个小红包的值的数组
     */
    public static long[] generate(long total, int count, long max, long min) {
        long[] result = new long[count];
        long average = total / count;
        for (int i = 0; i < result.length; i++) {
            //因为小红包的数量通常是要比大红包的数量要多的，因为这里的概率要调换过来。
            //当随机数>平均值，则产生小红包
            //当随机数<平均值，则产生大红包
            if (nextLong(min, max) > average) {
                // 在平均线上减钱
                long temp = min + xRandom(min, average);
                result[i] = temp;
                total -= temp;
            } else {
                // 在平均线上加钱
                long temp = max - xRandom(average, max);
                result[i] = temp;
                total -= temp;
            }
        }
        // 如果还有余钱，则尝试加到小红包里，如果加不进去，则尝试下一个。
        while (total > 0) {
            for (int i = 0; i < result.length; i++) {
                if (total > 0 && result[i] < max) {
                    result[i]++;
                    total--;
                }
            }
        }
        // 如果钱是负数了，还得从已生成的小红包中抽取回来
        while (total < 0) {
            for (int i = 0; i < result.length; i++) {
                if (total < 0 && result[i] > min) {
                    result[i]--;
                    total++;
                }
            }
        }
        return result;
    }


    public static long sqrt(long n) {
        return (long) Math.sqrt(n);
    }

    public static long sqr(long n) {
        return n * n;
    }

    public static long nextLong(long n) {
        return RandomUtils.nextLong(1, n);
    }

    public static long nextLong(long min, long max) {
        return RandomUtils.nextLong(min, max);
    }

    public static long maxValue(long[] array) {
        long maxIndex = array[0];
        //遍历循环数组
        for (int i = 0; i < array.length; i++) {
            if (maxIndex < array[i]) {
                maxIndex = array[i];
            }
        }
        return maxIndex;
    }
}
