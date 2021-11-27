package com.sonic.agent.tools;

import com.android.ddmlib.IDevice;
import com.sonic.agent.bridge.android.AndroidDeviceBridgeTool;
import com.sonic.agent.tests.android.AndroidTestTaskBootThread;
import com.sonic.agent.tests.android.AndroidTaskManager;
import com.sonic.agent.tests.android.mincap.InputSocketThread;
import com.sonic.agent.tests.android.mincap.OutputSocketThread;
import com.sonic.agent.tests.android.mincap.StartServerThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author ZhouYiXun
 * @des
 * @date 2021/8/26 9:20
 */
public class MiniCapTool {
    private final Logger logger = LoggerFactory.getLogger(MiniCapTool.class);

    public Thread start(
            String udId,
            AtomicReference<String[]> banner,
            AtomicReference<List<byte[]>> imgList,
            String pic,
            int tor,
            Session session
    ) {
        IDevice iDevice = AndroidDeviceBridgeTool.getIDeviceByUdId(udId);
        String key = String.format(AndroidTestTaskBootThread.ANDROID_TEST_TASK_BOOT_PRE, udId);
        int s;
        if (tor == -1) {
            s = AndroidDeviceBridgeTool.getScreen(AndroidDeviceBridgeTool.getIDeviceByUdId(udId));
        } else {
            s = tor;
        }
        int c = 0;
        switch (s) {
            case 0:
                c = 0;
                break;
            case 1:
                c = 90;
                break;
            case 2:
                c = 180;
                break;
            case 3:
                c = 270;
                break;
        }
        // int finalQua = qua;
        int finalC = c;
        // 启动mincap服务
        StartServerThread miniCapPro = new StartServerThread(iDevice, pic, finalC, session);
        AndroidTaskManager.startChildThread(key, miniCapPro);
        try {
            TimeUnit.SECONDS.sleep(4);
        } catch (InterruptedException e) {
            logger.error("启动miniCapPro等待过程中断");
            e.printStackTrace();
        }
        // 启动输入流
        InputSocketThread sendImg = new InputSocketThread(
                iDevice, new LinkedBlockingQueue<>(), miniCapPro
        );
        // 启动输出流
        OutputSocketThread outputSocketThread = new OutputSocketThread(
                sendImg, banner, imgList, session, pic, udId
        );

        AndroidTaskManager.startChildThread(key, sendImg, outputSocketThread);

        return miniCapPro; // server线程
    }

    private long bytesToLong(byte[] src, int offset) {
        long value;
        value = ((src[offset] & 0xFF) | ((src[offset + 1] & 0xFF) << 8) | ((src[offset + 2] & 0xFF) << 16)
                | ((src[offset + 3] & 0xFF) << 24));
        return value;
    }

    // java合并两个byte数组
    private byte[] addBytes(byte[] data1, byte[] data2) {
        byte[] data3 = new byte[data1.length + data2.length];
        System.arraycopy(data1, 0, data3, 0, data1.length);
        System.arraycopy(data2, 0, data3, data1.length, data2.length);
        return data3;
    }

    private byte[] subByteArray(byte[] byte1, int start, int end) {
        byte[] byte2 = new byte[0];
        try {
            byte2 = new byte[end - start];
        } catch (NegativeArraySizeException e) {
            e.printStackTrace();
        }
        System.arraycopy(byte1, start, byte2, 0, end - start);
        return byte2;
    }

    private void sendByte(Session session, byte[] message) {
        synchronized (session) {
            try {
                session.getBasicRemote().sendBinary(ByteBuffer.wrap(message));
            } catch (IllegalStateException | IOException e) {
                logger.error("socket发送失败!连接已关闭！");
            }
        }
    }

    private void sendText(Session session, String message) {
        synchronized (session) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IllegalStateException | IOException e) {
                logger.error("socket发送失败!连接已关闭！");
            }
        }
    }
}
