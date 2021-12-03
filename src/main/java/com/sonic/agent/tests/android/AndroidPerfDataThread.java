package com.sonic.agent.tests.android;

import com.sonic.agent.automation.AndroidStepHandler;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Android性能采集线程
 *
 * @author Eason(master) & JayWenStar(slave)
 * @date 2021/11/25 6:39 下午
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class AndroidPerfDataThread extends Thread {

    /**
     * 占用符逻辑参考：{@link AndroidTestTaskBootThread#ANDROID_TEST_TASK_BOOT_PRE}
     */
    @Setter(value = AccessLevel.NONE)
    public final static String ANDROID_PERF_DATA_TASK_PRE = "android-perf-data-task-%s-%s-%s";

    private final AndroidTestTaskBootThread androidTestTaskBootThread;

    public AndroidPerfDataThread(AndroidTestTaskBootThread androidTestTaskBootThread) {
        this.androidTestTaskBootThread = androidTestTaskBootThread;

        this.setDaemon(true);
        this.setName(androidTestTaskBootThread.formatThreadName(ANDROID_PERF_DATA_TASK_PRE));
    }

    /**
     * todo 这块可能需要修改
     */
    @Override
    public void run() {

        AndroidStepHandler androidStepHandler = androidTestTaskBootThread.getAndroidStepHandler();
        AndroidRunStepThread runStepThread = androidTestTaskBootThread.getRunStepThread();

        int tryTime = 0;
        while (runStepThread.isAlive()) {
            if (androidStepHandler.getAndroidDriver() == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.error("获取driver失败，错误信息{}" + e.getMessage());
                    e.printStackTrace();
                }
                continue;
            }
            try {
                androidStepHandler.getPerform();
                Thread.sleep(30000);
            } catch (Exception e) {
                tryTime++;
            }
            if (tryTime > 10) {
                break;
            }
        }
    }
}
