package com.sonic.agent.rocketmq.consumer;

import com.alibaba.fastjson.JSONObject;
import com.android.ddmlib.IDevice;
import com.sonic.agent.automation.AndroidStepHandler;
import com.sonic.agent.bridge.android.AndroidDeviceBridgeTool;
import com.sonic.agent.bridge.android.AndroidDeviceThreadPool;
import com.sonic.agent.bridge.ios.TIDeviceTool;
import com.sonic.agent.config.RocketMQConfig;
import com.sonic.agent.interfaces.PlatformType;
import com.sonic.agent.interfaces.ResultDetailStatus;
import com.sonic.agent.maps.AndroidPasswordMap;
import com.sonic.agent.maps.HandlerMap;
import com.sonic.agent.tests.android.AndroidTests;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.testng.TestNG;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试任务消费
 *
 * @author chenwenjie.star
 * @date 2021/11/23 7:59 下午
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "${rocketmq.topic.test-task-topic}",
        consumerGroup = "${rocketmq.group.test-task-group}",
        selectorExpression = "${sonic.agent.key}"
)
public class TestTaskConsumer implements RocketMQListener<JSONObject> {

    @Value("${spring.version}")
    private String version;
    @Value("${sonic.agent.host}")
    private String host;

    @Autowired
    private AndroidTests androidTests;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Autowired
    private RocketMQConfig rocketMQConfig;

    @Override
    public void onMessage(JSONObject jsonObject) {
        log.info("TestTaskConsumer消费者收到消息  : " + jsonObject.toString());
        AndroidDeviceThreadPool.cachedThreadPool.execute(() -> {
            switch (jsonObject.getString("msg")) {
                case "reboot":
                    if (jsonObject.getInteger("platform") == PlatformType.ANDROID) {
                        IDevice rebootDevice = AndroidDeviceBridgeTool.getIDeviceByUdId(jsonObject.getString("udId"));
                        if (rebootDevice != null) {
                            AndroidDeviceBridgeTool.reboot(rebootDevice);
                        }
                    }
                    if (jsonObject.getInteger("platform") == PlatformType.IOS) {
                        if (TIDeviceTool.getDeviceList().contains(jsonObject.getString("udId"))) {
                            TIDeviceTool.reboot(jsonObject.getString("udId"));
                        }
                    }
                    break;
                case "runStep":
                    if (jsonObject.getInteger("pf") == PlatformType.ANDROID) {
                        AndroidPasswordMap.getMap().put(jsonObject.getString("udId")
                                , jsonObject.getString("pwd"));
                        AndroidStepHandler androidStepHandler = HandlerMap.getAndroidMap().get(jsonObject.getString("sessionId"));
                        androidStepHandler.setGlobalParams(jsonObject.getJSONObject("gp"));
                        List<JSONObject> steps = jsonObject.getJSONArray("steps").toJavaList(JSONObject.class);
                        for (JSONObject step : steps) {
                            try {
                                androidStepHandler.runStep(step);
                            } catch (Throwable e) {
                                break;
                            }
                        }
                        androidStepHandler.setResultDetailStatus(ResultDetailStatus.PASS);
                        androidStepHandler.sendStatus();
                    }
                    break;
                case "suite":
//                    List<JSONObject> cases = jsonObject.getJSONArray("cases").toJavaList(JSONObject.class);
//                    TestNG tng = new TestNG();
//                    List<XmlSuite> suiteList = new ArrayList<>();
//                    XmlSuite xmlSuite = new XmlSuite();
//                    for (JSONObject dataInfo : cases) {
//                        XmlTest xmlTest = new XmlTest(xmlSuite);
//                        Map<String, String> parameters = new HashMap<>();
//                        parameters.put("dataInfo", dataInfo.toJSONString());
//                        xmlTest.setParameters(parameters);
//                        List<XmlClass> classes = new ArrayList<>();
//                        classes.add(new XmlClass(AndroidTests.class));
//                        xmlTest.setXmlClasses(classes);
//                    }
//                    suiteList.add(xmlSuite);
//                    tng.setXmlSuites(suiteList);
//                    tng.run();
//                    break;


                    JSONObject device = jsonObject.getJSONObject("device");
                    if (AndroidDeviceBridgeTool.getIDeviceByUdId(device.getString("udId")) != null) {
                        AndroidPasswordMap.getMap().put(device.getString("udId")
                                , device.getString("password"));
                        androidTests.run(jsonObject);
                    } else {
                        //取消本次测试
                        JSONObject subResultCount = new JSONObject();
                        subResultCount.put("rid", jsonObject.getInteger("rid"));
                        // todo 远程调用or事务
                        rocketMQTemplate.convertAndSend(
                                rocketMQConfig.getTopic().getTestDataTopic(),
                                subResultCount
                        );
                    }
                    break;
            }
        });
    }
}
