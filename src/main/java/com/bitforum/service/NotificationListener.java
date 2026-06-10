package com.bitforum.service;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.amqp.core.Message;

import com.bitforum.config.RabbitMQConfig;
import com.bitforum.message.ArticlePublishMessage;
import com.rabbitmq.client.Channel;

@Service
public class NotificationListener {
    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    @Autowired
    private RedisService redisService;

    // 监听文章发布队列。RabbitTemplate 发送 JSON 后，这里会自动反序列化成 ArticlePublishMessage。
    @RabbitListener(queues = RabbitMQConfig.ARTICLE_PUBLISH_QUEUE)
    public void handlePublish(ArticlePublishMessage message, Channel channel, Message rawMessage) throws IOException {
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();

        try {
            // 消费者先用 messageId 做幂等判断，避免 RabbitMQ 重投递时重复执行通知、积分等业务。
            boolean firstProcess = redisService.markMessageProcessed(message.getMessageId());
            if (!firstProcess) {
                log.info("文章发布消息已经过处理，跳过重复消费：messageId={}", message.getMessageId());
                // 重复消息已经没有继续处理的必要，直接 ack，告诉 RabbitMQ 不要再投递这条消息。
                channel.basicAck(deliveryTag, false);
                return;
            }

            log.info("收到文章发布消息：messageId={}, articleId={}, userId={}, title={}",
                    message.getMessageId(),
                    message.getArticleId(),
                    message.getUserId(),
                    message.getTitle());

            // 处理更新积分，发通知等耗时操作
            // 异步任务：发布后的慢活扔给线程池（不阻塞用户）
            Thread.sleep(2000); // 模拟耗时操作

            log.info("文章发布完成，主线程已返回（异步任务还在后台跑）");
            // 业务成功后手动 ack，明确告诉 RabbitMQ：这条消息已经处理完成。
            channel.basicAck(deliveryTag, false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("文章发布消息处理失败：messageId={}", message.getMessageId(), e);
            // 业务失败时 nack；requeue=false 表示不重新入队，避免失败消息无限重试。
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception e) {
            log.error("文章发布消息处理失败：messageId={}", message.getMessageId(), e);
            // 兜底处理非中断异常，例如 Redis 判断、通知逻辑等步骤出现异常。
            channel.basicNack(deliveryTag, false, false);
        }
    }

    // 监听死信队列。这里先只记录失败消息，后续可以扩展为人工补偿、告警或重新投递。
    @RabbitListener(queues = RabbitMQConfig.ARTICLE_PUBLISH_DLQ)
    public void handlePublishDeadLetter(ArticlePublishMessage message) {
        log.error("文章发布死信消息：messageId={}, articleId={}, userId={}, title={}",
                message.getMessageId(),
                message.getArticleId(),
                message.getUserId(),
                message.getTitle());
    }
}
