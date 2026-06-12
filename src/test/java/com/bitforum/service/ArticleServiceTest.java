package com.bitforum.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bitforum.config.RabbitMQConfig;
import com.bitforum.entity.Article;
import com.bitforum.entity.Comment;
import com.bitforum.mapper.ArticleMapper;
import com.bitforum.mapper.CommentMapper;
import com.bitforum.message.ArticlePublishMessage;

// 启动完整 Spring 容器，让 ArticleService、Mapper、事务等组件按真实项目方式协作
@SpringBootTest
// 测试结束后自动回滚数据库，不把测试文章留在真实表里。
@Transactional

public class ArticleServiceTest {
    @Autowired
    private ArticleService articleService;
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private CommentMapper commentMapper;
    // 不真的连接 RabbitMQ，只检查代码有没有调用“发送消息”。
    @MockitoBean
    private RabbitTemplate rabbitTemplate;
    @MockitoBean
    private RedisService redisService;

    @Test
    void publishShouldSaveArticleAndSendMessage() {
        Long userId = 10001L;
        // 让标题每次都不重复，避免数据库里已有同名文章影响测试。
        String title = "测试文章-" + UUID.randomUUID();
        String content = "这是一篇测试文章";

        articleService.publish(title, content, userId);

        // 发布后回查 MySQL，确认文章主流程确实把数据写入了 article 表
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.eq("title", title);
        Article article = articleMapper.selectOne(wrapper);

        assertNotNull(article);
        assertEquals(content, article.getContent());
        assertEquals(userId, article.getUserId());

        // 捕获发送给 RabbitMQ 的消息对象，再检查对象里的业务字段是否正确。
        ArgumentCaptor<ArticlePublishMessage> messageCaptor = ArgumentCaptor.forClass(ArticlePublishMessage.class);
        // Mockito 要求同一个方法调用里要么都用普通参数，要么都用 matcher；capture() 是 matcher，所以交换机和路由键也用 eq() 包起来。
        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMQConfig.ARTICLE_EXCHANGE),
            eq(RabbitMQConfig.ARTICLE_PUBLISH_ROUTING_KEY),
            messageCaptor.capture());
        ArticlePublishMessage message = messageCaptor.getValue();
        assertEquals(article.getId(), message.getArticleId());
        assertEquals(userId, message.getUserId());
        assertEquals(title, message.getTitle());
        assertNotNull(message.getMessageId());
        assertNotNull(message.getPublishTime());
    }

    @Test
    void nonAuthorShouldNotDeleteArticle() {
        Long authorId = 20001L;
        Long otherUserId = 20002L;

        // 先造一篇属于 authorId 的文章，用它来模拟真实的“文章归属关系”
        Article article = new Article();
        article.setTitle("权限测试文章-" + UUID.randomUUID());
        article.setContent("这是一篇测试非作者不能删除的文章");
        article.setUserId(authorId);
        articleMapper.insert(article);

        // otherUserId 不是作者，调用删除时应该被业务规则拦住并抛出异常
        RuntimeException exception = assertThrows(RuntimeException.class, ()->{
            articleService.delete(otherUserId, article.getId());
        });

        assertEquals("只能删除自己的文章", exception.getMessage());
        // 再查一次数据库，确认权限校验失败后文章没有被误删
        assertNotNull(articleMapper.selectById(article.getId()));
    }

    @Test
    void adminShouldDeleteArticleAndCleanRelatedData() {
        Article article = new Article();
        article.setTitle("管理员删除文章-" + UUID.randomUUID());
        article.setContent("管理员删除时应该同时清理评论和 Redis");
        article.setUserId(30001L);
        articleMapper.insert(article);

        Comment comment = new Comment();
        comment.setArticleId(article.getId());
        comment.setUserId(30002L);
        comment.setContent("这条评论应该跟随文章一起被删除");
        comment.setParentCommentId(0L);
        commentMapper.insert(comment);

        boolean deleted = articleService.deleteByAdmin(article.getId());

        assertTrue(deleted);
        assertNull(articleMapper.selectById(article.getId()));
        // 管理员删除文章复用统一清理流程，所以文章下的评论不能继续留在数据库里。
        assertEquals(0, commentMapper.selectCount(
                new QueryWrapper<Comment>().eq("article_id", article.getId())));
        verify(redisService).deleteArticleData(article.getId());
    }
}
