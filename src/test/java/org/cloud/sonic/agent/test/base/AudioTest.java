package org.cloud.sonic.agent.test.base;

import com.android.ddmlib.IDevice;
import org.cloud.sonic.agent.AgentApplication;
import org.cloud.sonic.agent.bridge.android.AndroidDeviceBridgeTool;
import org.cloud.sonic.agent.tools.AgentTool;
import org.cloud.sonic.agent.tools.PortTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Base api test.
 *
 * @author JayWenStar
 */
@SpringBootTest(
        classes = AgentApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public class AudioTest extends AbstractTestNGSpringContextTests {

    private final Logger logger = LoggerFactory.getLogger(AudioTest.class);

    @Test
    public void accTest() {
        String udId = "af80d1e4";
        IDevice iDevice = AndroidDeviceBridgeTool.getIDeviceByUdId(udId);
        AndroidDeviceBridgeTool.executeCommand(iDevice, "appops set org.cloud.sonic.android PROJECT_MEDIA allow");
        AndroidDeviceBridgeTool.executeCommand(iDevice, "am start -n org.cloud.sonic.android/.AudioActivity");
        AndroidDeviceBridgeTool.pressKey(iDevice, 4);
        int appListPort = PortTool.getPort();
        new Thread(()-> {
            try {
                AndroidDeviceBridgeTool.forward(iDevice, appListPort, "sonicaudioservice");
                Socket audioSocket = null;
                InputStream inputStream = null;
                FileOutputStream fos = new FileOutputStream("/Users/JayWenStar/Downloads/test.acc", true);
                FileChannel fc = fos.getChannel();
                try {
                    audioSocket = new Socket("localhost", appListPort);
                    inputStream = audioSocket.getInputStream();
                    int len = 1024;
                    while (audioSocket.isConnected()) {
                        byte[] buffer = new byte[len];
                        int realLen;
                        realLen = inputStream.read(buffer);
                        if (buffer.length != realLen && realLen >= 0) {
                            buffer = AgentTool.subByteArray(buffer, 0, realLen);
                        }
                        if (realLen >= 0) {
                            ByteBuffer byteBuffer = ByteBuffer.allocate(buffer.length);
                            byteBuffer.put(byteBuffer);
                            byteBuffer.flip();
                            fc.write(byteBuffer);
                            fos.flush();
                            // sendText(session, byteBuffer);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (audioSocket != null && audioSocket.isConnected()) {
                        try {
                            audioSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            AndroidDeviceBridgeTool.removeForward(iDevice, appListPort, "sonicaudioservice");
        }).run(); //
    }


}
