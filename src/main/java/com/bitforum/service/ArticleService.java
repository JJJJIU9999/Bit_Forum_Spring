package com.bitforum.service;

import com.bitforum.config.RabbitMQConfig;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bitforum.entity.Article;
import com.bitforum.mapper.ArticleMapper;
import com.bitforum.message.ArticlePublishMessage;

@Service
public class ArticleService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private CommentService commentService;
    @Autowired
    private RedisService redisService;

    private static final Logger log = LoggerFactory.getLogger(ArticleService.class);

    public void publish(String title, String content, Long userId) {
        //主流程：保存文章（同步，必须保证完成）
        Article article = new Article();
        article.setTitle(title);
        article.setContent(content);
        article.setUserId(userId);
        articleMapper.insert(article);

        // 插入数据库后，MyBatis-Plus 会把自增主键回填到 article.id，消息里就可以带上真实文章 ID。
        ArticlePublishMessage articlePublishMessage = new ArticlePublishMessage();
        articlePublishMessage.setArticleId(article.getId());
        articlePublishMessage.setUserId(userId);
        articlePublishMessage.setTitle(title);
        articlePublishMessage.setPublishTime(System.currentTimeMillis());
        // UUID 用来生成全局唯一的消息编号，为后续消费者幂等判断做准备。
        articlePublishMessage.setMessageId(UUID.randomUUID().toString());
        // 发送到指定交换机，再由 routingKey 路由到文章发布队列，消费者仍然只监听最终队列。
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.ARTICLE_EXCHANGE,
            RabbitMQConfig.ARTICLE_PUBLISH_ROUTING_KEY,
            articlePublishMessage);
        log.info("文章发送完成，MQ消息已发送");

    }
    
    public List<Article> listAll() {
        List<Article> articleList = articleMapper.selectList(null);
        log.info("已有的文章如下：{}", articleList);
        return articleList;
    }

    public Page<Article> pageArticles(long pageNum, long pageSize) {
        // Page 对象里放分页参数：当前页 pageNum、每页条数 pageSize
        Page<Article> page = new Page<>(pageNum,pageSize);
        // selectPage 会查询当前页数据，并把总条数、总页数等分页信息写回 Page 对象
        return articleMapper.selectPage(page, null);
    }

    public Article findById(Long id) {
        log.info("ID为{}的文章如下：", id);
        return articleMapper.selectById(id);
    }
    
    public void update(Long userId, Long articleId, String title, String content) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        if (!article.getUserId().equals(userId)) {
            throw new RuntimeException("只能修改自己的文章");
        }
        article.setTitle(title);
        article.setContent(content);
        articleMapper.updateById(article);
    }
    
    @Transactional
    public void delete(Long userId,Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        if (!article.getUserId().equals(userId)) {
            throw new RuntimeException("只能删除自己的文章");
        }
        // 删除文章是一个完整业务流程：先清评论，再删文章，最后清 Redis 缓存数据
        commentService.deleteByArticleId(articleId);
        articleMapper.deleteById(articleId);
        redisService.deleteArticleData(articleId);
    }

}
