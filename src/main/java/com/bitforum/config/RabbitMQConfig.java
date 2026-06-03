package com.bitforum.config;

import org.springframework.amqp.core.Queue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//RabbitMQ 就是个消息中转站，队列是通道，监听器是办事员。核心价值是把"发送方"和"处理方"完全分开。
@Configuration
public class RabbitMQConfig {
    // 提前在 RabbitMQ 里建好队列。
    @Bean
    public Queue articlePublishQueue() {
        return new Queue("article.publish.queue", true);
    }
}
