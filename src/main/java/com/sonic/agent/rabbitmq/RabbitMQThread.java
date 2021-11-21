package com.sonic.agent.rabbitmq;

import com.alibaba.fastjson.JSONObject;
import com.sonic.agent.tools.AgentTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * updater: chenwenjie.star
 *
 * todo 如果后续确认RocketMq有异步机制，那就这里的代码就不需要了
 */
@Component
public class RabbitMQThread {
    private static LinkedBlockingQueue<JSONObject> msgQueue;
    public static ExecutorService cachedThreadPool;
    public static boolean isPass = false;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Bean
    public void rabbitMsgInit() {
        cachedThreadPool = Executors.newCachedThreadPool();
        msgQueue = new LinkedBlockingQueue<>();
    }

    public static void send(JSONObject jsonObject) {
        msgQueue.offer(jsonObject);
    }

    @Bean
    public void sendToRabbitMQ() {
        cachedThreadPool.submit(() -> {
            while (true) {
                if (!isPass) {
                    Thread.sleep(5000);
                    continue;
                }
                try {
                    if (!msgQueue.isEmpty()) {
                        JSONObject m = msgQueue.poll();
                        m.put("agentId", AgentTool.agentId);
                        // todo rocketmq
                        rabbitTemplate.convertAndSend("DataExchange", "data", m);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
