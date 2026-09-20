package com.bitforum.ai.rag;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bitforum.config.RabbitMQConfig;
import com.bitforum.entity.Article;
import com.bitforum.mapper.ArticleMapper;
import com.bitforum.message.KbIndexMessage;
import com.bitforum.service.ArticleService;
import com.bitforum.service.RedisService;
import com.rabbitmq.client.Channel;

/**
 * 知识库索引消息监听器（M15）。
 *
 * <p>复用既有的「手动 ACK + 死信队列 + Redis 幂等」套路（与 {@code NotificationListener} 一致）：
 *
 * <ul>
 *   <li>处理成功 → 标记消息已处理并 ACK；</li>
 *   <li>重复投递 → 直接 ACK，不重复计算向量；</li>
 *   <li>处理异常 → {@code basicNack(requeue=false)}，消息进入知识库专属死信队列，
 *       不会影响文章发布通知那条队列（findings.md T8）。</li>
 * </ul>
 *
 * <p><b>为什么消费者要重新查文章状态</b>：消息只带 articleId，索引还是移除由消费时的
 * 文章当前状态决定。这样「审核通过 / 下架 / 删除」三种触发点可以共用一种消息，
 * 也天然支持消息堆积后按最新状态收敛。
 */
@Service
public class KbIndexMessageListener {

    private static final Logger log = LoggerFactory.getLogger(KbIndexMessageListener.class);

    @Autowired
    private RedisService redisService;
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private KbIndexService kbIndexService;

    @RabbitListener(queues = RabbitMQConfig.KB_INDEX_QUEUE)
    public void handleKbIndex(KbIndexMessage message, Channel channel, Message rawMessage) throws IOException {
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();

        try {
            if (redisService.isMessageProcessed(message.getMessageId())) {
                log.info("知识库索引消息已处理过，跳过重复消费：messageId={}", message.getMessageId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            Article article = articleMapper.selectById(message.getArticleId());
            if (article != null && ArticleService.STATUS_PUBLISHED.equals(article.getStatus())) {
                kbIndexService.indexArticle(article);
            } else {
                // 文章已删除或已不在已发布状态：把它的向量移出知识库
                kbIndexService.removeArticle(message.getArticleId());
            }

            redisService.markMessageProcessed(message.getMessageId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("知识库索引消息处理失败：messageId={}, articleId={}",
                    message == null ? null : message.getMessageId(),
                    message == null ? null : message.getArticleId(),
                    e);
            // requeue=false：立即进入死信队列，避免同一条坏消息无限重投把队列堵死
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /** 死信队列只做记录：知识库可以随时用管理员的全量重建接口补齐。 */
    @RabbitListener(queues = RabbitMQConfig.KB_INDEX_DLQ)
    public void handleKbIndexDeadLetter(KbIndexMessage message) {
        log.error("知识库索引死信消息：messageId={}, articleId={}",
                message == null ? null : message.getMessageId(),
                message == null ? null : message.getArticleId());
    }
}
