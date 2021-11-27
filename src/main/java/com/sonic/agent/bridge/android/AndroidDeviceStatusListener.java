package com.sonic.agent.bridge.android;

import com.alibaba.fastjson.JSONObject;
import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.sonic.agent.config.RocketMQConfig;
import com.sonic.agent.interfaces.PlatformType;
import com.sonic.agent.maps.AndroidDeviceManagerMap;
import com.sonic.agent.netty.NettyThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * @author ZhouYiXun
 * @des adb上下线监听，发送对应给server
 * @date 2021/08/16 19:26
 */
@Component
@Slf4j
public class AndroidDeviceStatusListener implements AndroidDebugBridge.IDeviceChangeListener, ApplicationContextAware {

    private RocketMQTemplate rocketMQTemplate;
    private RocketMQConfig rocketMQConfig;


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        rocketMQTemplate = applicationContext.getBean(RocketMQTemplate.class);
        rocketMQConfig = applicationContext.getBean(RocketMQConfig.class);
        log.info(rocketMQTemplate.toString());
        log.info(rocketMQConfig.toString());
    }

    /**
     * @param device
     * @return void
     * @author ZhouYiXun
     * @des 发送设备状态
     * @date 2021/8/16 19:58
     */
    private void send(IDevice device) {
        JSONObject deviceDetail = new JSONObject();
        deviceDetail.put("msg", "deviceDetail");
        deviceDetail.put("udId", device.getSerialNumber());
        deviceDetail.put("name", device.getProperty("ro.product.name"));
        deviceDetail.put("model", device.getProperty(IDevice.PROP_DEVICE_MODEL));
        deviceDetail.put("status", device.getState());
        deviceDetail.put("platform", PlatformType.ANDROID);
        deviceDetail.put("version", device.getProperty(IDevice.PROP_BUILD_VERSION));
        deviceDetail.put("size", AndroidDeviceBridgeTool.getScreenSize(device));
        deviceDetail.put("cpu", device.getProperty(IDevice.PROP_DEVICE_CPU_ABI));
        deviceDetail.put("manufacturer", device.getProperty(IDevice.PROP_DEVICE_MANUFACTURER));

        // todo 这里最好用远程调用
        NettyThreadPool.send(deviceDetail);
    }

    @Override
    public void deviceConnected(IDevice device) {
        log.info("Android设备：" + device.getSerialNumber() + " ONLINE！");
        AndroidDeviceManagerMap.getMap().remove(device.getSerialNumber());
        send(device);
    }

    @Override
    public void deviceDisconnected(IDevice device) {
        log.info("Android设备：" + device.getSerialNumber() + " OFFLINE！");
        AndroidDeviceManagerMap.getMap().remove(device.getSerialNumber());
        send(device);
    }

    @Override
    public void deviceChanged(IDevice device, int changeMask) {
        if (device.isOnline()) {
            log.info("Android设备：" + device.getSerialNumber() + " ONLINE！");
        } else {
            log.info("Android设备：" + device.getSerialNumber() + " OFFLINE！");
        }
        send(device);
    }
}