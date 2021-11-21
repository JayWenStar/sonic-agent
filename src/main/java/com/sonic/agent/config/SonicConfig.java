package com.sonic.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author chenwenjie.star
 * @date 2021/11/24 6:41 下午
 */
@Component
@ConfigurationProperties(prefix = "sonic")
@Data
public class SonicConfig {

    private Agent agent;

    private Server server;

    @Data
    public static class Agent {

        /**
         * agent端ip
         */
        private String host;

        private int port;

        /**
         * 需先启动server+前端获取
         */
        private String key;

    }

    @Data
    public static class Server {

        /**
         * server服务的ip（如果是有网关就填网关ip）
         */
        private String host;

        /**
         * server端服务端口
         */
        private int folderPort;

        /**
         * server端netty端口
         */
        private int transportPort;

    }


}
