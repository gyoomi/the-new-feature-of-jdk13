/**
 * Copyright © 2019, Glodon Digital Supplier BU.
 * <p>
 * All Rights Reserved.
 */

package com.gyoomi.jdk13;

/**
 * The description of class
 *
 * @author Leon
 * @date 2019-09-25 10:13
 */
public class SwitchDemo {

    public static void main(String[] args) {
        // JDK13的写法1
        // 如果没有default 则不进行任何输出
        int variable = new Integer(args.length < 1 || args[0] == null ? "0" : args[0]);
        switch (variable) {
            case 1, 2, 3 -> System.out.println("小");
            case 4, 5, 6 -> System.out.println("中");
            case 7, 8, 9 -> System.out.println("大");
            default -> System.out.println("超出范围，无法识别");
        }

        System.out.println("***************************");
        // JDK13写法2（带返回值）
        int variable2 = 5;
        String result = switch (variable2) {
            case 1, 2, 3 -> "小";
            case 4, 5, 6 -> "中";
            case 7, 8, 9 -> "大";
            default -> "超出范围，无法识别";
        };
        System.out.println(result);

        System.out.println("***************************");
        // JDK13,JDK12之前的古老写法
        int variable3 = 8;
        switch (variable3) {
            case 1,2,3:
                System.out.println("小3");
                break;
            case 4,5,6:
                System.out.println("中3");
                break;
            case 7,8,9:
                System.out.println("大3");
                // 必须要加上break，而JDK12,JDK13中则不需要
                break;
            default:
                System.out.println("其他意外情况3");
        }
    }
}
