package com.bitforum.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.bitforum.entity.Article;
import com.bitforum.mapper.ArticleMapper;

@Service
public class ArticleService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final Logger log = LoggerFactory.getLogger(ArticleService.class);

    public void publish(String title, String content, Long userId) {
        //主流程：保存文章（同步，必须保证完成）
        Article article = new Article();
        article.setTitle(title);
        article.setContent(content);
        article.setUserId(userId);
        articleMapper.insert(article);

        //  convert — 把消息体自动转成字节（你传的 "文章ID：11" 是个字符串，Spring 帮你序列化）
        //  send — 发到 RabbitMQ 的 article.publish.queue 这条队列上
        rabbitTemplate.convertAndSend("article.publish.queue", "文章ID：" + article.getId());
        log.info("文章发送完成，MQ消息已发送");

    }
    
    public List<Article> listAll() {
        List<Article> articleList = articleMapper.selectList(null);
        log.info("已有的文章如下：{}", articleList);
        return articleList;
    }

    public Article findById(Long id) {
        log.info("ID为{}的文章如下：", id);
        return articleMapper.selectById(id);
    }
    
    public void update(Long userId,Long articleId,String title, String content) {
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
    
    public void delete(Long userId,Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        if (!article.getUserId().equals(userId)) {
            throw new RuntimeException("只能删除自己的文章");
        }
        articleMapper.deleteById(articleId);
    }

}
