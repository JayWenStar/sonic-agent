package com.sonic.agent.netty;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.android.ddmlib.IDevice;
import com.sonic.agent.automation.AndroidStepHandler;
import com.sonic.agent.bridge.android.AndroidDeviceBridgeTool;
import com.sonic.agent.bridge.ios.LibIMobileDeviceTool;
import com.sonic.agent.interfaces.PlatformType;
import com.sonic.agent.maps.AndroidPasswordMap;
import com.sonic.agent.maps.HandlerMap;
import com.sonic.agent.tests.AndroidTests;
import com.sonic.agent.tools.SpringTool;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NettyClientHandler extends ChannelInboundHandlerAdapter {
    private final Logger logger = LoggerFactory.getLogger(NettyClientHandler.class);
    private static Map<String, Session> sessionMap = new ConcurrentHashMap<String, Session>();
    private NettyClient nettyClient;
    public static Channel channel = null;
    private AndroidTests androidTests = SpringTool.getBean(AndroidTests.class);

    public NettyClientHandler(NettyClient nettyClient, Channel channel) {
        this.nettyClient = nettyClient;
        this.channel = channel;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        logger.info("Agent:{} 连接到服务器 {} 成功!", ctx.channel().localAddress(), ctx.channel().remoteAddress());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        JSONObject jsonObject = JSON.parseObject((String) msg);
        logger.info("Agent:{} 收到服务器 {} 消息: {}", ctx.channel().localAddress(), ctx.channel().remoteAddress(), jsonObject);
        NettyThreadPool.cachedThreadPool.execute(() -> {
            switch (jsonObject.getString("msg")) {
                case "reboot":
                    if (jsonObject.getInteger("platform") == PlatformType.ANDROID) {
                        IDevice rebootDevice = AndroidDeviceBridgeTool.getIDeviceByUdId(jsonObject.getString("udId"));
                        if (rebootDevice != null) {
                            AndroidDeviceBridgeTool.reboot(rebootDevice);
                        }
                    }
                    if (jsonObject.getInteger("platform") == PlatformType.IOS) {
                        if (LibIMobileDeviceTool.getDeviceList().contains(jsonObject.getString("udId"))) {
                            LibIMobileDeviceTool.reboot(jsonObject.getString("udId"));
                        }
                    }
                    break;
                case "heartBeat":
                    JSONObject heartBeat = new JSONObject();
                    heartBeat.put("msg", "heartBeat");
                    heartBeat.put("status", "alive");
                    NettyThreadPool.send(heartBeat);
                    break;
                case "runStep":
                    if (jsonObject.getInteger("pf") == PlatformType.ANDROID) {
                        AndroidPasswordMap.getMap().put(jsonObject.getString("udId")
                                , jsonObject.getString("pwd"));
                        AndroidStepHandler androidStepHandler = HandlerMap.getAndroidMap().get(jsonObject.getString("sessionId"));
                        androidStepHandler.resetResultDetailStatus();
                        androidStepHandler.setGlobalParams(jsonObject.getJSONObject("gp"));
                        List<JSONObject> steps = jsonObject.getJSONArray("steps").toJavaList(JSONObject.class);
                        for (JSONObject step : steps) {
                            try {
                                androidStepHandler.runStep(step);
                            } catch (Throwable e) {
                                break;
                            }
                        }
                        androidStepHandler.sendStatus();
                    }
                    break;
                case "suite":
                    JSONObject device = jsonObject.getJSONObject("device");
                    if (AndroidDeviceBridgeTool.getIDeviceByUdId(device.getString("udId")) != null) {
                        AndroidPasswordMap.getMap().put(device.getString("udId")
                                , device.getString("password"));
                        try {
                            androidTests.run(jsonObject);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        //取消本次测试
                        JSONObject subResultCount = new JSONObject();
                        subResultCount.put("rid", jsonObject.getInteger("rid"));
                        NettyThreadPool.send(subResultCount);
                    }
                    break;
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.info("服务器: {} 发生异常 {}", ctx.channel().remoteAddress(), cause.fillInStackTrace());
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("服务器: {} 连接断开", ctx.channel().remoteAddress());
        NettyThreadPool.isPassSecurity = false;
        if (channel != null) {
            channel.close();
        }
        channel = null;
        nettyClient.doConnect();
    }

    public static Map<String, Session> getMap() {
        return sessionMap;
    }
}