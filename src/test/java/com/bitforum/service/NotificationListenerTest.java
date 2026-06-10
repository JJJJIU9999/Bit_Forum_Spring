package com.bitforum.service;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import com.bitforum.message.ArticlePublishMessage;
import com.rabbitmq.client.Channel;

@ExtendWith(MockitoExtension.class)
public class NotificationListenerTest {

    @InjectMocks
    private NotificationListener notificationListener;

    @Mock
    private RedisService redisService;

    @Mock
    private Channel channel;

    @Test
    void duplicateMessageShouldAckAndSkip() throws Exception {
        ArticlePublishMessage message = new ArticlePublishMessage();
        message.setMessageId("duplicate-message-id");

        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setDeliveryTag(123L);
        Message rawMessage = new Message(new byte[0], messageProperties);

        // Redis 返回 false，代表这个 messageId 已经处理过，监听器应该跳过业务并 ack 掉消息。
        when(redisService.markMessageProcessed(message.getMessageId())).thenReturn(false);

        notificationListener.handlePublish(message, channel, rawMessage);

        verify(channel).basicAck(123L, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }
}
