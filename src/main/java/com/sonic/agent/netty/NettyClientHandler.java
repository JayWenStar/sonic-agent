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
import com.sonic.agent.tests.android.AndroidTests;
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

/**
 * @updater: chenwenjie.star
 *
 * netty客户端消息处理
 */
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
                case "heartBeat":
                    JSONObject heartBeat = new JSONObject();
                    heartBeat.put("msg", "heartBeat");
                    heartBeat.put("status", "alive");
                    NettyThreadPool.send(heartBeat);
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