package com.sonic.agent.tests.android.mincap;

import com.android.ddmlib.IDevice;
import com.sonic.agent.bridge.android.AndroidDeviceBridgeTool;
import com.sonic.agent.tools.PortTool;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Queue;

/**
 * mincap socket线程
 * 通过端口转发，将设备视频流转发到此Socket
 *
 * @author chenwenjie.star
 * @date 2021/11/25 11:52 下午
 */
@Data
@Slf4j
public class InputSocketThread extends Thread {

    @Setter(value = AccessLevel.NONE)
    public final static String ANDROID_INPUT_SOCKET_PRE = "android-input-socket-task-%s";

    private IDevice iDevice;

    private Queue<byte[]> dataQueue;

    private StartServerThread miniCapPro;

    public InputSocketThread(IDevice iDevice, Queue<byte[]> dataQueue, StartServerThread miniCapPro) {
        this.iDevice = iDevice;
        this.dataQueue = dataQueue;
        this.miniCapPro = miniCapPro;
        String udId = iDevice.getSerialNumber();

        this.setDaemon(true);
        this.setName(String.format(ANDROID_INPUT_SOCKET_PRE, udId));
    }


    @Override
    public void run() {
        try {
            miniCapPro.getDoneSemaphore().acquire();
        } catch (InterruptedException e) {
            log.error("mincap初始化出现异常！");
            e.printStackTrace();
        }

        int finalMiniCapPort = PortTool.getPort();
        AndroidDeviceBridgeTool.forward(iDevice, finalMiniCapPort, "minicap");
        Socket capSocket = null;
        InputStream inputStream = null;
        try {
            capSocket = new Socket("localhost", finalMiniCapPort);
            inputStream = capSocket.getInputStream();
            while (miniCapPro.isAlive()) {
                byte[] buffer;
                int len = 0;
                while (len == 0) {
                    len = inputStream.available();
                }
                buffer = new byte[len];
                inputStream.read(buffer);
                dataQueue.add(buffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (miniCapPro.isAlive()) {
                miniCapPro.interrupt();
                log.info("miniCap thread已关闭");
            }
            if (capSocket != null && capSocket.isConnected()) {
                try {
                    capSocket.close();
                    log.info("miniCap socket已关闭");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                    log.info("miniCap input流已关闭");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        AndroidDeviceBridgeTool.removeForward(iDevice, finalMiniCapPort, "minicap");
    }
}
