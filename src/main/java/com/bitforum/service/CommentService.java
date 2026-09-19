package com.bitforum.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bitforum.ai.trace.TraceHeaders;
import com.bitforum.config.RabbitMQConfig;
import com.bitforum.entity.Article;
import com.bitforum.entity.Comment;
import com.bitforum.mapper.ArticleMapper;
import com.bitforum.mapper.CommentMapper;
import com.bitforum.message.AfterCommitExecutor;
import com.bitforum.message.ModerationMessage;

@Service
public class CommentService {
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private AfterCommitExecutor afterCommitExecutor;

    private static final Logger log = LoggerFactory.getLogger(CommentService.class);

    //发表评论
    public boolean publish(Long userId, Long articleId, String content) {
        // 评论是文章的从属数据，插入评论前必须先确认文章存在
        // 这里直接用 ArticleMapper 查询，避免 CommentService 和 ArticleService 互相注入形成循环依赖
        Article article = articleMapper.selectById(articleId);
        if (article == null || !ArticleService.STATUS_PUBLISHED.equals(article.getStatus())) {
            // 这里先返回 false，让 Controller 决定给前端什么业务提示
            return false;
        }
        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setArticleId(articleId);
        comment.setContent(content);
        comment.setParentCommentId(0L); //0表示“一级评论”
        log.info("发表评论成功,发表评论的用户ID是{}",userId);
        commentMapper.insert(comment);
        notificationService.notifyComment(article, userId, content);
        // M16：评论发布即公开，AI 只能做事后检测 —— 生成待处理记录提醒管理员，
        // 绝不删除或隐藏已发布的评论（见 task_plan.md 的 M16 实施决策）。
        sendModerationMessage(comment.getId());
        // 插入成功后返回 true，表示 Controller 可以返回“评论发布成功”
        return true;
    }

    /**
     * 发送评论审核消息（M16）。
     *
     * <p>事务提交后投递；MQ 故障不影响评论发布 —— 漏掉的消息只意味着该评论
     * 回到"没有 AI 检测"的状态，由用户举报等既有机制兜底。
     */
    private void sendModerationMessage(Long commentId) {
        afterCommitExecutor.run(() -> doSendModerationMessage(commentId));
    }

    private void doSendModerationMessage(Long commentId) {
        ModerationMessage message = new ModerationMessage();
        message.setTargetType(ModerationMessage.TARGET_COMMENT);
        message.setTargetId(commentId);
        message.setMessageId(UUID.randomUUID().toString());
        try {
            // M18：与文章审核消息同样的处理：若当前线程有执行轨迹，就把 traceId 带上
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ARTICLE_EXCHANGE,
                    RabbitMQConfig.MODERATION_ROUTING_KEY,
                    message,
                    TraceHeaders.propagate());
        } catch (RuntimeException e) {
            log.error("评论审核消息发送失败：commentId={}", commentId, e);
        }
    }

    //查看某篇文章的所有评论
    public List<Comment> listByArticleId(Long articleId) {
        QueryWrapper<Comment> wrapper = new QueryWrapper<>();
        wrapper.eq("article_id", articleId).orderByAsc("create_time");
        List<Comment> commentList = commentMapper.selectList(wrapper);
        log.info("文章的所有评论如下：{}", commentList);
        return commentList;
    }

    public void deleteByArticleId(Long articleId) {
        // 删除文章时，先按 article_id 清理这篇文章下的所有评论
        QueryWrapper<Comment> wrapper = new QueryWrapper<>();
        wrapper.eq("article_id", articleId);
        commentMapper.delete(wrapper);
    }

    public Page<Comment> pageComments(long pageNum, long pageSize) {
        // 管理端查看全部评论，不按某篇文章过滤；按时间倒序更方便优先处理新评论。
        Page<Comment> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Comment> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time");
        return commentMapper.selectPage(page, wrapper);
    }

    public boolean deleteByAdmin(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            return false;
        }
        // 评论本身是独立记录，管理员删除评论只需要删除 comment 表记录。
        commentMapper.deleteById(commentId);
        return true;
    }
}
