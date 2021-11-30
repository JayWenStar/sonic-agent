package com.sonic.agent.tools;

import com.android.ddmlib.IDevice;
import com.sonic.agent.bridge.android.AndroidDeviceBridgeTool;
import com.sonic.agent.tests.android.AndroidTaskManager;
import com.sonic.agent.tests.android.AndroidTestTaskBootThread;
import com.sonic.agent.tests.android.mincap.InputSocketThread;
import com.sonic.agent.tests.android.mincap.OutputSocketThread;
import com.sonic.agent.tests.android.mincap.StartServerThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.sonic.agent.tests.android.AndroidTestTaskBootThread.ANDROID_TEST_TASK_BOOT_PRE;

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
            Session session,
            AndroidTestTaskBootThread androidTestTaskBootThread
    ) {
        IDevice iDevice = AndroidDeviceBridgeTool.getIDeviceByUdId(udId);
        String key = androidTestTaskBootThread.formatThreadName(ANDROID_TEST_TASK_BOOT_PRE);
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
        StartServerThread miniCapPro = new StartServerThread(iDevice, pic, finalC, session, androidTestTaskBootThread);
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
                sendImg, banner, imgList, session, pic
        );

        AndroidTaskManager.startChildThread(key, sendImg, outputSocketThread);

        return miniCapPro; // server线程
    }
}
