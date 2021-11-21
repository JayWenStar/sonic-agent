package com.sonic.agent.tests.android.mincap;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.SyncException;
import com.android.ddmlib.TimeoutException;
import com.sonic.agent.bridge.android.AndroidDeviceBridgeTool;
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
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class StartServerThread extends Thread {

    @Setter(value = AccessLevel.NONE)
    public final static String ANDROID_START_MINCAP_SERVER_PRE = "android-start-mincap-server-task-%s";

    private IDevice iDevice;

    private String pic;

    private int finalC;

    private Session session;

    private String udId;

    private Semaphore doneSemaphore = new Semaphore(1);


    public StartServerThread(IDevice iDevice, String pic, int finalC, Session session) {
        this.iDevice = iDevice;
        this.pic = pic;
        this.finalC = finalC;
        this.session = session;
        this.udId = iDevice.getSerialNumber();

        this.setDaemon(true);
        this.setName(String.format(ANDROID_START_MINCAP_SERVER_PRE, udId));
    }

    @Override
    public void run() {
        try {
            AndroidDeviceBridgeTool.startMiniCapServer(iDevice, pic, finalC, session);
            Thread.sleep(4000);
            doneSemaphore.release();
        } catch (AdbCommandRejectedException | IOException | SyncException | TimeoutException | InterruptedException e) {
            e.printStackTrace();
            doneSemaphore.release();
        }
    }
}
