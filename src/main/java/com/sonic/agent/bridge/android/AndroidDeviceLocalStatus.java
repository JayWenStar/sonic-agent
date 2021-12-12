package com.sonic.agent.bridge.android;

import com.alibaba.fastjson.JSONObject;
import com.sonic.agent.config.RocketMQConfig;
import com.sonic.agent.interfaces.DeviceStatus;
import com.sonic.agent.maps.AndroidDeviceManagerMap;
import com.sonic.agent.netty.NettyThreadPool;
import com.sonic.agent.tests.android.AndroidTestTaskBootThread;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author ZhouYiXun
 * @des 本地自定义的状态变更，不是通过adb的变更
 * @date 2021/08/16 19:26
 */
public class AndroidDeviceLocalStatus implements ApplicationContextAware {

    private static RocketMQConfig rocketMQConfig;
    private static RocketMQTemplate rocketMQTemplate;
    private static Semaphore semaphore = new Semaphore(1);

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        rocketMQConfig = applicationContext.getBean(RocketMQConfig.class);
        rocketMQTemplate = applicationContext.getBean(RocketMQTemplate.class);
    }


    /**
     * @param udId
     * @param status
     * @return void
     * @author ZhouYiXun
     * @des 发送状态变更消息
     * @date 2021/8/16 20:56
     */
    public static void send(String udId, String status) {
        JSONObject deviceDetail = new JSONObject();
        deviceDetail.put("msg", "deviceDetail");
        deviceDetail.put("udId", udId);
        deviceDetail.put("status", status);
        NettyThreadPool.send(deviceDetail);
    }

    /**
     * @param udId
     * @return void
     * @author ZhouYiXun
     * @des 开始测试
     * @date 2021/8/16 20:57
     */
    public static boolean startTest(String udId) throws InterruptedException {
        semaphore.acquire();
        System.out.println("当前线程为： " + Thread.currentThread());
        System.out.println("getMap()内容为：" + AndroidDeviceManagerMap.getMap().get(udId));
        if (!AndroidDeviceManagerMap.getMap().containsKey(udId)) {
            send(udId, DeviceStatus.TESTING);
            AndroidDeviceManagerMap.getMap().put(udId, DeviceStatus.TESTING);
            semaphore.release();
            return true;
        } else {
            semaphore.release();
            return false;
        }
    }

    /**
     * @param udId
     * @return void
     * @author ZhouYiXun
     * @des 开始调试
     * @date 2021/8/16 20:57
     */
    public static void startDebug(String udId) {
        send(udId, DeviceStatus.DEBUGGING);
        AndroidDeviceManagerMap.getMap().put(udId, DeviceStatus.DEBUGGING);
    }

    /**
     * @param udId
     * @return void
     * @author ZhouYiXun
     * @des 使用完毕
     * @date 2021/8/16 19:47
     */
    public static void finish(String udId) {
        if (AndroidDeviceBridgeTool.getIDeviceByUdId(udId) != null
                && AndroidDeviceManagerMap.getMap().get(udId) != null) {
            if (AndroidDeviceManagerMap.getMap().get(udId).equals(DeviceStatus.DEBUGGING)
                    || AndroidDeviceManagerMap.getMap().get(udId).equals(DeviceStatus.TESTING)) {
                send(udId, DeviceStatus.ONLINE);
            }
        }
        AndroidDeviceManagerMap.getMap().remove(udId);
    }

    /**
     * @param udId
     * @return void
     * @author ZhouYiXun
     * @des 使用完毕异常
     * @date 2021/8/16 19:47
     */
    public static void finishError(String udId) {
        if (AndroidDeviceBridgeTool.getIDeviceByUdId(udId) != null
                && AndroidDeviceManagerMap.getMap().get(udId) != null) {
            if (AndroidDeviceManagerMap.getMap().get(udId).equals(DeviceStatus.DEBUGGING)
                    || AndroidDeviceManagerMap.getMap().get(udId).equals(DeviceStatus.TESTING)) {
                send(udId, DeviceStatus.ERROR);
            }
        }
        AndroidDeviceManagerMap.getMap().remove(udId);
    }
}
