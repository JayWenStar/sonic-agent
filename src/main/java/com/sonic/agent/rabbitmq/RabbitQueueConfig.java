//package com.sonic.agent.rabbitmq;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.UUID;
//
///**
// * @updater: chenwenjie.star
// *
// * rabbitmq配置类
// * todo 确认无误后删除
// *
// */
//@Configuration
//public class RabbitQueueConfig {
//    private final Logger logger = LoggerFactory.getLogger(RabbitQueueConfig.class);
//    private String queueId;
//    @Value("${sonic.agent.key}")
//    private String key;
//
//    @Bean
//    public String queueId() {
//        queueId = UUID.randomUUID().toString();
//        return queueId;
//    }
//
//    /**
//     * 消息-交换机
//     */
//    @Bean("MsgDirectExchange")
//    public DirectExchange MsgDirectExchange() {
//        return new DirectExchange("MsgDirectExchange", true, false);
//    }
//
//    /**
//     * 消息-队列
//     */
//    @Bean("MsgQueue")
//    public Queue MsgQueue() {
//        return new Queue("MsgQueue-" + key, true);
//    }
//
//    /**
//     * 任务-交换机
//     */
//    @Bean("TaskDirectExchange")
//    public DirectExchange TaskDirectExchange() {
//        return new DirectExchange("TaskDirectExchange", true, false);
//    }
//
//    /**
//     * 任务-队列
//     */
//    @Bean("TaskQueue")
//    public Queue TaskQueue() {
//        Map<String, Object> params = new HashMap<>();
//        params.put("x-message-ttl", 1000 * 60 * 5);
//        // 绑定死信交换机
//        params.put("x-dead-letter-exchange", "MsgDirectExchange");
//        // 绑定死信交换机路由key
//        params.put("x-dead-letter-routing-key", key);
//        return QueueBuilder.durable("TaskQueue-" + key).withArguments(params).build();
//    }
//
//    @Bean
//    public Binding bindingMsgDirect(@Qualifier("MsgQueue") Queue queue,
//                                    @Qualifier("MsgDirectExchange") DirectExchange exchange) {
//        return BindingBuilder.bind(queue).to(exchange).with(key);
//    }
//
//    @Bean
//    public Binding bindingTaskDirect(@Qualifier("TaskQueue") Queue queue,
//                                     @Qualifier("TaskDirectExchange") DirectExchange exchange) {
//        return BindingBuilder.bind(queue).to(exchange).with(key);
//    }
//
//    @Bean
//    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
//        RabbitTemplate rabbitTemplate = new RabbitTemplate();
//        rabbitTemplate.setConnectionFactory(connectionFactory);
//        rabbitTemplate.setMandatory(true);
//        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
//        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
//            if (!ack) {
//                logger.error("ConfirmCallback: 相关数据：" + correlationData + "\n" +
//                        "确认情况：" + false + "\n" +
//                        "原因：" + cause);
//            }
//        });
//
//        rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
//                logger.info("ConfirmCallback: 消息：" + message + "\n" +
//                        "回应码：" + replyCode + "\n" +
//                        "回应信息：" + replyText + "\n" +
//                        "交换机：" + exchange + "\n" +
//                        "路由键：" + routingKey);
//        });
//        return rabbitTemplate;
//    }
//
//}
