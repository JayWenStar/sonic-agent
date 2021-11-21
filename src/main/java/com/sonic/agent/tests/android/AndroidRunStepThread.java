package com.sonic.agent.tests.android;

import com.alibaba.fastjson.JSONObject;
import com.sonic.agent.automation.AndroidStepHandler;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 步骤任务线程
 *
 * @author chenwenjie.star
 * @date 2021/11/25 10:34 上午
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class AndroidRunStepThread extends Thread {

    @Setter(value = AccessLevel.NONE)
    public final static String ANDROID_RUN_STEP_TASK_PRE = "android-run-step-task-%s";

    private final AndroidTaskBootThread androidTaskBootThread;

    public AndroidRunStepThread(AndroidTaskBootThread androidTaskBootThread) {
        this.androidTaskBootThread = androidTaskBootThread;
        String udId = androidTaskBootThread.getUdId();

        this.setDaemon(true);
        this.setName(String.format(ANDROID_RUN_STEP_TASK_PRE, udId));
    }

    @Override
    public void run() {

        // todo 等待性能线程准备

        JSONObject jsonObject = androidTaskBootThread.getJsonObject();
        AndroidStepHandler androidStepHandler = androidTaskBootThread.getAndroidStepHandler();
        List<JSONObject> steps = jsonObject.getJSONArray("steps").toJavaList(JSONObject.class);

        for (JSONObject step : steps) {
            try {
                androidStepHandler.runStep(step);
            } catch (Throwable e) {
                break;
            }
        }

    }
}
