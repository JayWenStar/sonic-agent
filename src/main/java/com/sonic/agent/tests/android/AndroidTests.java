package com.sonic.agent.tests.android;

import com.alibaba.fastjson.JSONObject;
import com.sonic.agent.automation.AndroidStepHandler;
import com.sonic.agent.interfaces.DeviceStatus;
import com.sonic.agent.interfaces.ResultDetailStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 安卓测试执行类
 *
 * @author Eason(master) & JayWenStar(slave)
 * @date 2021/11/23 20:15 下午
 */
@Component
@Slf4j
public class AndroidTests {

    /**
     * mq策略
     */
    public void run(JSONObject jsonObject) {
        AndroidStepHandler androidStepHandler = new AndroidStepHandler();
        int rid = jsonObject.getInteger("rid");
        int cid = jsonObject.getInteger("cid");
        String udId = jsonObject.getJSONObject("device").getString("udId");
        JSONObject gp = jsonObject.getJSONObject("gp");
        androidStepHandler.setGlobalParams(gp);
        androidStepHandler.setTestMode(cid, rid, udId, DeviceStatus.TESTING, "");

        androidStepHandler.setResultDetailStatusForce(ResultDetailStatus.RUNNING);
        androidStepHandler.sendStatus();
        androidStepHandler.resetResultDetailStatus();

        // 启动任务
        AndroidTestTaskBootThread bootThread = new AndroidTestTaskBootThread(jsonObject, androidStepHandler);
        AndroidTaskManager.startBootThread(bootThread);
        // todo 远程调用 or 消息回调
        // todo 考虑下如果在这之后停止服务器
    }
}
