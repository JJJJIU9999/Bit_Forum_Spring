package com.bitforum.ai.rag;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import com.bitforum.entity.Article;
import com.bitforum.mapper.ArticleMapper;
import com.bitforum.message.KbIndexMessage;
import com.bitforum.service.ArticleService;
import com.bitforum.service.RedisService;
import com.rabbitmq.client.Channel;

/**
 * 知识库索引消息监听器测试（M15）。
 *
 * <p>验证手动 ACK 与幂等的完整分支：正常索引、下架与删除时的移除、重复消息跳过、
 * 异常时进入死信队列（nack 且不重回队列）。
 */
@ExtendWith(MockitoExtension.class)
class KbIndexMessageListenerTest {

    @InjectMocks
    private KbIndexMessageListener listener;

    @Mock
    private RedisService redisService;
    @Mock
    private ArticleMapper articleMapper;
    @Mock
    private KbIndexService kbIndexService;
    @Mock
    private Channel channel;

    @Test
    void publishedArticleShouldBeIndexedThenMarkedAndAcked() throws Exception {
        KbIndexMessage message = message("kb-normal", 901L);
        Message raw = rawMessage(11L);
        when(redisService.isMessageProcessed("kb-normal")).thenReturn(false);
        when(articleMapper.selectById(901L)).thenReturn(article(901L, ArticleService.STATUS_PUBLISHED));

        listener.handleKbIndex(message, channel, raw);

        InOrder order = inOrder(redisService, kbIndexService, channel);
        order.verify(redisService).isMessageProcessed("kb-normal");
        order.verify(kbIndexService).indexArticle(org.mockito.ArgumentMatchers.any(Article.class));
        order.verify(redisService).markMessageProcessed("kb-normal");
        order.verify(channel).basicAck(11L, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void offlineArticleShouldBeRemovedFromKnowledgeBase() throws Exception {
        KbIndexMessage message = message("kb-offline", 902L);
        Message raw = rawMessage(12L);
        when(redisService.isMessageProcessed("kb-offline")).thenReturn(false);
        when(articleMapper.selectById(902L)).thenReturn(article(902L, ArticleService.STATUS_OFFLINE));

        listener.handleKbIndex(message, channel, raw);

        verify(kbIndexService).removeArticle(902L);
        verify(kbIndexService, never()).indexArticle(org.mockito.ArgumentMatchers.any(Article.class));
        verify(channel).basicAck(12L, false);
    }

    @Test
    void deletedArticleShouldBeRemovedFromKnowledgeBase() throws Exception {
        KbIndexMessage message = message("kb-deleted", 903L);
        Message raw = rawMessage(13L);
        when(redisService.isMessageProcessed("kb-deleted")).thenReturn(false);
        when(articleMapper.selectById(903L)).thenReturn(null);

        listener.handleKbIndex(message, channel, raw);

        verify(kbIndexService).removeArticle(903L);
        verify(channel).basicAck(13L, false);
    }

    @Test
    void duplicateMessageShouldOnlyAck() throws Exception {
        KbIndexMessage message = message("kb-duplicate", 904L);
        Message raw = rawMessage(14L);
        when(redisService.isMessageProcessed("kb-duplicate")).thenReturn(true);

        listener.handleKbIndex(message, channel, raw);

        verify(channel).basicAck(14L, false);
        verifyNoInteractions(kbIndexService);
        verify(articleMapper, never()).selectById(org.mockito.ArgumentMatchers.anyLong());
        verify(redisService, never()).markMessageProcessed(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void failedIndexShouldNackToDeadLetterQueue() throws Exception {
        KbIndexMessage message = message("kb-failed", 905L);
        Message raw = rawMessage(15L);
        when(redisService.isMessageProcessed("kb-failed")).thenReturn(false);
        when(articleMapper.selectById(905L)).thenReturn(article(905L, ArticleService.STATUS_PUBLISHED));
        doThrow(new IllegalStateException("向量库不可用"))
                .when(kbIndexService).indexArticle(org.mockito.ArgumentMatchers.any(Article.class));

        listener.handleKbIndex(message, channel, raw);

        // requeue=false：立即进入死信队列，避免坏消息反复重投堵住队列
        verify(channel).basicNack(15L, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(redisService, never()).markMessageProcessed(org.mockito.ArgumentMatchers.anyString());
    }

    private KbIndexMessage message(String messageId, Long articleId) {
        KbIndexMessage message = new KbIndexMessage();
        message.setMessageId(messageId);
        message.setArticleId(articleId);
        return message;
    }

    private Article article(Long id, String status) {
        Article article = new Article();
        article.setId(id);
        article.setStatus(status);
        article.setTitle("测试文章");
        return article;
    }

    private Message rawMessage(long deliveryTag) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(deliveryTag);
        return new Message(new byte[0], properties);
    }
}
