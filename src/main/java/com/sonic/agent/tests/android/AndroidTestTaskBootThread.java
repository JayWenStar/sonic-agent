package com.sonic.agent.tests.android;

import com.alibaba.fastjson.JSONObject;
import com.sonic.agent.automation.AndroidStepHandler;
import com.sonic.agent.bridge.android.AndroidDeviceLocalStatus;
import com.sonic.agent.config.RocketMQConfig;
import com.sonic.agent.config.SonicConfig;
import com.sonic.agent.interfaces.ResultDetailStatus;
import com.sonic.agent.rocketmq.enums.MessageDelayLevel;
import com.sonic.agent.tests.TaskManager;
import com.sonic.agent.tools.SpringTool;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;

import java.util.concurrent.Semaphore;

/**
 * Android测试任务线程，会用 {@link Thread#interrupt()} 来强制停止任务
 *
 * @author Eason(master) & JayWenStar(slave)
 * @date 2021/11/24 6:15 下午
 */
@Slf4j
@Getter
@Setter
@Accessors(chain = true)
public class AndroidTestTaskBootThread extends Thread {

    /**
     * android-test-task-boot-{resultId}-{caseId}-{udid}
     */
    @Setter(value = AccessLevel.NONE)
    public final static String ANDROID_TEST_TASK_BOOT_PRE = "android-test-task-boot-%s-%s-%s";

    /**
     * 控制不同线程执行的信号量
     */
    @Setter(value = AccessLevel.NONE)
    private Semaphore runStepSemaphore = new Semaphore(1);


    /**
     * 一些任务信息
     */
    private JSONObject jsonObject;

    /**
     * Android步骤处理器，包含一些状态信息
     */
    private AndroidStepHandler androidStepHandler;

    /**
     * 测试步骤线程
     */
    private AndroidRunStepThread runStepThread;

    /**
     * 性能数据采集线程
     */
    private AndroidPerfDataThread perfDataThread;

    /**
     * 录像线程
     */
    private AndroidRecordThread recordThread;

    /**
     * 测试结果id 0表示debug线程
     */
    private int resultId = 0;

    /**
     * 测试用例id 0表示debug线程
     */
    private int caseId = 0;

    /**
     * 设备序列号
     */
    private String udId;

    public String formatThreadName(String baseFormat) {
        return String.format(baseFormat, this.resultId, this.caseId, this.udId);
    }

    /**
     * debug线程构造
     */
    public AndroidTestTaskBootThread() {
        this.setName(this.formatThreadName(ANDROID_TEST_TASK_BOOT_PRE));
        this.setDaemon(true);
    }

    /**
     * 任务线程构造
     *
     * @param jsonObject          任务数据
     * @param androidStepHandler  android步骤执行器
     */
    public AndroidTestTaskBootThread(JSONObject jsonObject, AndroidStepHandler androidStepHandler) {
        this.androidStepHandler = androidStepHandler;
        this.jsonObject = jsonObject;
        this.resultId = jsonObject.getInteger("rid");
        this.caseId = jsonObject.getInteger("cid");
        this.udId = jsonObject.getJSONObject("device").getString("udId");

        // 比如：test-task-thread-af80d1e4
        this.setName(String.format(ANDROID_TEST_TASK_BOOT_PRE, resultId, caseId, udId));
        this.setDaemon(true);
    }

    @Override
    public void run() {

        try {
            RocketMQTemplate rocketMQTemplate = SpringTool.getBean(RocketMQTemplate.class);
            RocketMQConfig rocketMQConfig = SpringTool.getBean(RocketMQConfig.class);
            SonicConfig sonicConfig = SpringTool.getBean(SonicConfig.class);

            String udId = jsonObject.getJSONObject("device").getString("udId");
            String key = sonicConfig.getAgent().getKey();

            int wait = jsonObject.getInteger("wait");
            if (!AndroidDeviceLocalStatus.startTest(udId)) {
                androidStepHandler.waitDevice(wait + 1);
                wait++;
                if (wait >= 30) {
                    androidStepHandler.waitDeviceTimeOut(udId);
                    androidStepHandler.sendStatus();
                } else {
                    //延时队列 todo 延时策略变更
                    jsonObject.put("wait", wait);
                    rocketMQTemplate.syncSend(
                            rocketMQConfig.getTopic().getTestTaskTopic() + ":" + key,
                            MessageBuilder.withPayload(jsonObject).build(),
                            rocketMQTemplate.getProducer().getSendMsgTimeout(),
                            MessageDelayLevel.TIME_1M.level
                    );
                    log.info("任务进入延时队列，1min后重试:" + jsonObject);
                }
                return;
            }

            //启动测试
            try {
                androidStepHandler.startAndroidDriver(udId);
            } catch (Exception e) {
                log.error(e.getMessage());
                e.printStackTrace();
                androidStepHandler.closeAndroidDriver();
                androidStepHandler.sendStatus();
                AndroidDeviceLocalStatus.finishError(udId);
                return;
            }

            //电量过低退出测试
            if (androidStepHandler.getBattery()) {
                androidStepHandler.closeAndroidDriver();
                androidStepHandler.sendStatus();
                AndroidDeviceLocalStatus.finish(udId);
                return;
            }

            //正常运行步骤的线程
            runStepThread = new AndroidRunStepThread(this);
            //性能数据获取线程
            perfDataThread = new AndroidPerfDataThread(this);
            //录像线程
            recordThread = new AndroidRecordThread(this);
            TaskManager.startChildThread(this.getName(), runStepThread, perfDataThread, recordThread);


            //等待两个线程结束了才结束方法
            while ((recordThread.isAlive()) || (runStepThread.isAlive())) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            log.error("任务异常，中断：{}", e.getMessage());
            e.printStackTrace();
            androidStepHandler.setResultDetailStatus(ResultDetailStatus.FAIL);
        } finally {
            AndroidDeviceLocalStatus.finish(udId);
            androidStepHandler.closeAndroidDriver();
            androidStepHandler.sendStatus();
        }
    }
}
