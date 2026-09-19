package com.bitforum.ai.moderation;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bitforum.config.RabbitMQConfig;
import com.bitforum.entity.Article;
import com.bitforum.entity.Comment;
import com.bitforum.mapper.ArticleMapper;
import com.bitforum.mapper.CommentMapper;
import com.bitforum.message.ModerationMessage;
import com.bitforum.service.ArticleService;
import com.bitforum.service.RedisService;
import com.rabbitmq.client.Channel;

/**
 * 内容审核消息监听器（M16）。
 *
 * <p>复用既有的「手动 ACK + 死信队列 + Redis 幂等」套路（与通知、知识库索引一致）：
 * 处理成功则标记并 ACK，重复投递直接 ACK，异常则 nack 进死信队列。
 *
 * <p><b>消费者回查内容而不是信任消息</b>：消息只带对象 id，消费时按 id 取最新内容。
 * 这对文章尤其重要 —— 只审核**仍处于待审核状态**的文章：
 *
 * <ul>
 *   <li>若管理员已经手动处理过，再跑一次 AI 分析没有意义；</li>
 *   <li>更重要的是，自动放行会调用 {@code approve}，而它要求文章处于 PENDING，
 *       状态不对时虽然会被降级处理，但先判状态可以避免无谓的模型调用与失败日志。</li>
 * </ul>
 */
@Service
public class ModerationMessageListener {

    private static final Logger log = LoggerFactory.getLogger(ModerationMessageListener.class);

    @Autowired
    private RedisService redisService;
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private ModerationService moderationService;

    @RabbitListener(queues = RabbitMQConfig.MODERATION_QUEUE)
    public void handleModeration(ModerationMessage message, Channel channel, Message rawMessage) throws IOException {
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();

        try {
            if (redisService.isMessageProcessed(message.getMessageId())) {
                log.info("审核消息已处理过，跳过重复消费：messageId={}", message.getMessageId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            boolean analyzed = analyze(message);
            redisService.markMessageProcessed(message.getMessageId());
            channel.basicAck(deliveryTag, false);

            if (!analyzed) {
                log.info("审核消息已消费但不产生分析结果：messageId={}, type={}, targetId={}",
                        message.getMessageId(), message.getTargetType(), message.getTargetId());
            }
        } catch (Exception e) {
            log.error("审核消息处理失败：messageId={}, type={}, targetId={}",
                    message == null ? null : message.getMessageId(),
                    message == null ? null : message.getTargetType(),
                    message == null ? null : message.getTargetId(),
                    e);
            // requeue=false：立即进入死信队列，避免坏消息反复重投堵塞队列
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 按对象类型回查内容并送审。
     *
     * @return 是否真正执行了分析（对象不存在或状态不匹配时返回 false）
     */
    private boolean analyze(ModerationMessage message) {
        if (ModerationMessage.TARGET_ARTICLE.equals(message.getTargetType())) {
            Article article = articleMapper.selectById(message.getTargetId());
            if (article == null) {
                log.info("待审核文章不存在，跳过：articleId={}", message.getTargetId());
                return false;
            }
            if (!ArticleService.STATUS_PENDING.equals(article.getStatus())) {
                log.info("文章已不在待审核状态，跳过 AI 审核：articleId={}, status={}",
                        article.getId(), article.getStatus());
                return false;
            }
            // 标题同样参与审核：标题党的风险往往比正文更明显
            String content = article.getTitle() + "\n" + article.getContent();
            moderationService.moderate(
                    ModerationTargetType.ARTICLE, article.getId(), content, article.getUserId());
            return true;
        }

        if (ModerationMessage.TARGET_COMMENT.equals(message.getTargetType())) {
            Comment comment = commentMapper.selectById(message.getTargetId());
            if (comment == null) {
                log.info("待审核评论不存在（可能已删除），跳过：commentId={}", message.getTargetId());
                return false;
            }
            moderationService.moderate(
                    ModerationTargetType.COMMENT, comment.getId(), comment.getContent(), comment.getUserId());
            return true;
        }

        log.warn("未知的审核对象类型，跳过：type={}", message.getTargetType());
        return false;
    }

    /** 死信队列只做记录：漏掉的审核不影响内容发布，人工审核流程仍然兜底。 */
    @RabbitListener(queues = RabbitMQConfig.MODERATION_DLQ)
    public void handleModerationDeadLetter(ModerationMessage message) {
        log.error("审核死信消息：messageId={}, type={}, targetId={}",
                message == null ? null : message.getMessageId(),
                message == null ? null : message.getTargetType(),
                message == null ? null : message.getTargetId());
    }
}
