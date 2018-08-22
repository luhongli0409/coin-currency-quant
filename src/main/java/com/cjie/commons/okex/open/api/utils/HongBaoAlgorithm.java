package com.cjie.commons.okex.open.api.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * @Author: hongli.lu
 * @Date: 2018/8/22 下午2:48
 */

public class HongBaoAlgorithm {

    private static Random random = new Random();

    static {
        random.setSeed(System.currentTimeMillis());
    }

    public static void main1(String[] args) {
        long max = 20;
        long min = 1;
        long[] result = generate(100, 10, max, min);
        long total = 0;
        for (int i = 0; i < result.length; i++) {
            total += result[i];
        }
        //检查生成的红包的总额是否正确
        System.out.println("total:" + total);

        for (int i = 0; i < result.length; i++) {
            System.out.println(result[i]);
        }
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
        return sqrt(nextLong(sqr(max - min)));
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
        return random.nextInt((int) n);
    }

    public static long nextLong(long min, long max) {
        return random.nextInt((int) (max - min + 1)) + min;
    }

    /**
     *    * 计算每人获得红包金额;最小每人0.01元
     *    * @param mmm 红包总额
     *    * @param number 人数
     *    * @return
     *    
     */
    public static List<BigDecimal> math(BigDecimal mmm, int number) {
        if (mmm.doubleValue() < number * 0.01) {
            return null;
        }
        Random random = new Random();
        // 金钱，按分计算 10块等于 1000分
        int money = mmm.multiply(BigDecimal.valueOf(100)).intValue();
        // 随机数总额
        double count = 0;
        // 每人获得随机点数
        double[] arrRandom = new double[number];
        // 每人获得钱数
        List<BigDecimal> arrMoney = new ArrayList<>(number);
        // 循环人数 随机点
        for (int i = 0; i < arrRandom.length; i++) {
            int r = random.nextInt((number) * 99) + 1;
            count += r;
            arrRandom[i] = r;
        }
        // 计算每人拆红包获得金额
        int c = 0;
        for (int i = 0; i < arrRandom.length; i++) {
            // 每人获得随机数相加 计算每人占百分比
            Double x = new Double(arrRandom[i] / count);
            // 每人通过百分比获得金额
            int m = (int) Math.floor(x * money);
            // 如果获得 0 金额，则设置最小值 1分钱
            if (m == 0) {
                m = 1;
            }
            // 计算获得总额
            c += m;
            // 如果不是最后一个人则正常计算
            if (i < arrRandom.length - 1) {
                arrMoney.add(new BigDecimal(m).divide(new BigDecimal(100)));
            } else {
                // 如果是最后一个人，则把剩余的钱数给最后一个人
                arrMoney.add(new BigDecimal(money - c + m).divide(new BigDecimal(100)));
            }
        }
        // 随机打乱每人获得金额
        Collections.shuffle(arrMoney);
        return arrMoney;
    }

    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            List<BigDecimal> moneys = math(BigDecimal.valueOf(10), 6);
            if (moneys != null) {
                BigDecimal b = new BigDecimal(0);
                for (BigDecimal bigDecimal : moneys) {
                    System.out.print(bigDecimal + "元  ");
                    b = b.add(bigDecimal);
                }
                System.out.print("  总额：" + b + "元 ");
                System.out.println();
            }
        }
    }
}
