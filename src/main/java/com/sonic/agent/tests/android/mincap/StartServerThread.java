package com.sonic.agent.tests.android.mincap;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.SyncException;
import com.android.ddmlib.TimeoutException;
import com.sonic.agent.bridge.android.AndroidDeviceBridgeTool;
import com.sonic.agent.tests.android.AndroidTestTaskBootThread;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.websocket.Session;
import java.io.IOException;
import java.util.concurrent.Semaphore;

/**
 * 启动mincap server线程
 *
 * @author chenwenjie.star
 * @date 2021/11/25 8:35 下午
 */
@Data
@Slf4j
public class StartServerThread extends Thread {

    /**
     * 占用符逻辑参考：{@link AndroidTestTaskBootThread#ANDROID_TEST_TASK_BOOT_PRE}
     */
    @Setter(value = AccessLevel.NONE)
    public final static String ANDROID_START_MINCAP_SERVER_PRE = "android-start-mincap-server-task-%s-%s-%s";

    private IDevice iDevice;

    private String pic;

    private int finalC;

    private Session session;

    private String udId;

    private AndroidTestTaskBootThread androidTestTaskBootThread;


    public StartServerThread(IDevice iDevice, String pic, int finalC, Session session,
                             AndroidTestTaskBootThread androidTestTaskBootThread) {
        this.iDevice = iDevice;
        this.pic = pic;
        this.finalC = finalC;
        this.session = session;
        this.udId = iDevice.getSerialNumber();
        this.androidTestTaskBootThread = androidTestTaskBootThread;

        this.setDaemon(true);
        this.setName(androidTestTaskBootThread.formatThreadName(ANDROID_START_MINCAP_SERVER_PRE));
    }

    @Override
    public void run() {
        try {
            AndroidDeviceBridgeTool.startMiniCapServer(iDevice, pic, finalC, session);
            Thread.sleep(4000);
        } catch (AdbCommandRejectedException | IOException | SyncException | TimeoutException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
