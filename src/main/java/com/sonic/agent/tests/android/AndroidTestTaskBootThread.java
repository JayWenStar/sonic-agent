package com.sonic.agent.tests.android;

import com.alibaba.fastjson.JSONObject;
import com.sonic.agent.automation.AndroidStepHandler;
import com.sonic.agent.bridge.android.AndroidDeviceLocalStatus;
import com.sonic.agent.config.RocketMQConfig;
import com.sonic.agent.config.SonicConfig;
import com.sonic.agent.interfaces.ResultDetailStatus;
import com.sonic.agent.rocketmq.enums.MessageDelayLevel;
import com.sonic.agent.tools.SpringTool;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;

import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * Android测试任务线程，会用 {@link Thread#interrupt()} 来强制停止任务
 *
 * @author chenwenjie.star
 * @date 2021/11/24 6:15 下午
 */
@Slf4j
@Getter
@Setter
public class AndroidTestTaskBootThread extends Thread {

    @Setter(value = AccessLevel.NONE)
    public final static String ANDROID_TEST_TASK_BOOT_PRE = "android-test-task-boot-%s";

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
     * 设备序列号
     */
    private String udId;

    public AndroidTestTaskBootThread(JSONObject jsonObject, AndroidStepHandler androidStepHandler) {
        this.androidStepHandler = androidStepHandler;
        this.jsonObject = jsonObject;
        this.udId = jsonObject.getJSONObject("device").getString("udId");

        // 比如：test-task-thread-af80d1e4
        this.setName(String.format(ANDROID_TEST_TASK_BOOT_PRE, udId));
        this.setDaemon(true);
    }

    @Override
    public void run() {

        RocketMQTemplate rocketMQTemplate = SpringTool.getBean(RocketMQTemplate.class);
        RocketMQConfig rocketMQConfig = SpringTool.getBean(RocketMQConfig.class);
        SonicConfig sonicConfig = SpringTool.getBean(SonicConfig.class);

        String udId = jsonObject.getJSONObject("device").getString("udId");
        String key = sonicConfig.getAgent().getKey();

        int wait = jsonObject.getInteger("wait");
        if (!AndroidDeviceLocalStatus.startTest(udId)) {
            androidStepHandler.waitDevice(wait + 1);
            wait++;
            if (wait >= 24) {
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
                log.info("进入延时队列:" + jsonObject);
            }
            return;
        }

        //启动测试
        try {
            androidStepHandler.startAndroidDriver(udId);
        } catch (Exception e) {
            log.error(e.getMessage());
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
        AndroidTaskManager.startChildThread(this.getName(), runStepThread, perfDataThread, recordThread);


        //等待两个线程结束了才结束方法
        // todo 注意如果线程被强制停止，也同样的结束状态，注意下有没有特殊情况（比如关闭一组线程的时候应该先把boot线程先干掉）
        while ((recordThread.isAlive()) || (runStepThread.isAlive())) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }
        }
        androidStepHandler.closeAndroidDriver();
        androidStepHandler.sendStatus();
        AndroidDeviceLocalStatus.finish(udId);

    }
}
