package com.sonic.agent.interfaces;

/**
 * 测试结果状态枚举，与Server的一致
 *
 * @updater chenwenjie.star
 * @date    2021-11-28 16:19
 */
public interface ResultDetailStatus {

    /**
     * 任务分发中
     */
    int DISTRIBUTING = -1;

    /**
     * 任务进行中
     */
    int RUNNING = 0;
    /**
     * 测试通过
     */
    int PASS = 1;
    /**
     * 中断警告
     */
    int WARN = 2;
    /**
     * 测试失败
     */
    int FAIL = 3;
}
