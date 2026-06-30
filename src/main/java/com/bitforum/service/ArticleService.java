package com.bitforum.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bitforum.config.RabbitMQConfig;
import com.bitforum.entity.Article;
import com.bitforum.entity.ArticleAuditRecord;
import com.bitforum.entity.ArticleFavorite;
import com.bitforum.entity.Category;
import com.bitforum.mapper.ArticleAuditRecordMapper;
import com.bitforum.mapper.ArticleFavoriteMapper;
import com.bitforum.mapper.ArticleMapper;
import com.bitforum.mapper.CategoryMapper;
import com.bitforum.message.ArticlePublishMessage;

@Service
public class ArticleService {
    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_OFFLINE = "OFFLINE";

    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private ArticleAuditRecordMapper articleAuditRecordMapper;
    @Autowired
    private ArticleFavoriteMapper articleFavoriteMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private CommentService commentService;
    @Autowired
    private RedisService redisService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private NotificationService notificationService;

    private static final Logger log = LoggerFactory.getLogger(ArticleService.class);

    public void publish(String title, String content, Long categoryId, Long userId) {
        createArticle(title, content, categoryId, userId, STATUS_PENDING);
        log.info("文章已提交审核");
    }

    public Article saveDraft(String title, String content, Long categoryId, Long userId) {
        return createArticle(title, content, categoryId, userId, STATUS_DRAFT);
    }

    public void updateDraft(Long userId, Long articleId, String title, String content, Long categoryId) {
        Article article = findOwnedArticle(userId, articleId);
        if (!STATUS_DRAFT.equals(article.getStatus()) && !STATUS_REJECTED.equals(article.getStatus())) {
            throw new RuntimeException("只能修改草稿或被驳回文章");
        }
        Category category = categoryService.findEnabledById(categoryId);
        if (category == null) {
            throw new RuntimeException("板块不存在或已禁用");
        }

        article.setTitle(title);
        article.setContent(content);
        article.setCategoryId(categoryId);
        article.setStatus(STATUS_DRAFT);
        articleMapper.updateById(article);
    }

    public void submit(Long userId, Long articleId) {
        Article article = findOwnedArticle(userId, articleId);
        if (!STATUS_DRAFT.equals(article.getStatus()) && !STATUS_REJECTED.equals(article.getStatus())) {
            throw new RuntimeException("只能提交草稿或被驳回文章");
        }
        Category category = categoryService.findEnabledById(article.getCategoryId());
        if (category == null) {
            throw new RuntimeException("板块不存在或已禁用");
        }
        article.setStatus(STATUS_PENDING);
        articleMapper.updateById(article);
    }

    public List<Article> listAll() {
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.eq("status", STATUS_PUBLISHED);
        wrapper.orderByDesc("create_time");
        List<Article> articleList = articleMapper.selectList(wrapper);
        fillArticleMetadata(articleList);
        return articleList;
    }

    public Page<Article> pageArticles(long pageNum, long pageSize) {
        return pageArticles(pageNum, pageSize, null);
    }

    public Page<Article> pageArticles(long pageNum, long pageSize, Long categoryId) {
        Page<Article> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.eq("status", STATUS_PUBLISHED);
        if (categoryId != null) {
            wrapper.eq("category_id", categoryId);
        }
        wrapper.orderByDesc("create_time");
        Page<Article> result = articleMapper.selectPage(page, wrapper);
        fillArticleMetadata(result.getRecords());
        return result;
    }

    public Page<Article> searchPublishedArticles(String keyword, Long categoryId, long pageNum, long pageSize) {
        Page<Article> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.eq("status", STATUS_PUBLISHED);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query.like("title", keyword).or().like("content", keyword));
        }
        if (categoryId != null) {
            wrapper.eq("category_id", categoryId);
        }
        wrapper.orderByDesc("create_time");
        Page<Article> result = articleMapper.selectPage(page, wrapper);
        fillArticleMetadata(result.getRecords());
        return result;
    }

    public Page<Article> pageUserArticles(Long userId, long pageNum, long pageSize, String status) {
        Page<Article> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        if (StringUtils.hasText(status)) {
            validateStatus(status);
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("create_time");
        Page<Article> result = articleMapper.selectPage(page, wrapper);
        fillArticleMetadata(result.getRecords());
        return result;
    }

    public Page<Article> pageAdminArticles(long pageNum, long pageSize, Long categoryId) {
        Page<Article> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq("category_id", categoryId);
        }
        wrapper.orderByDesc("create_time");
        Page<Article> result = articleMapper.selectPage(page, wrapper);
        fillArticleMetadata(result.getRecords());
        return result;
    }

    public Page<Article> pageAuditArticles(long pageNum, long pageSize, String status) {
        String auditStatus = StringUtils.hasText(status) ? status : STATUS_PENDING;
        validateStatus(auditStatus);

        Page<Article> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.eq("status", auditStatus);
        wrapper.orderByDesc("create_time");
        Page<Article> result = articleMapper.selectPage(page, wrapper);
        fillArticleMetadata(result.getRecords());
        return result;
    }

    public Article findById(Long id) {
        Article article = articleMapper.selectById(id);
        fillArticleMetadata(article);
        return article;
    }

    public Article findPublishedById(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null || !STATUS_PUBLISHED.equals(article.getStatus())) {
            return null;
        }
        fillArticleMetadata(article);
        return article;
    }

    @Transactional
    public void favoriteArticle(Long userId, Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null || !STATUS_PUBLISHED.equals(article.getStatus())) {
            throw new RuntimeException("只能收藏已发布文章");
        }
        Long count = articleFavoriteMapper.selectCount(new QueryWrapper<ArticleFavorite>()
                .eq("user_id", userId)
                .eq("article_id", articleId));
        if (count > 0) {
            throw new RuntimeException("不能重复收藏同一篇文章");
        }

        ArticleFavorite favorite = new ArticleFavorite();
        favorite.setUserId(userId);
        favorite.setArticleId(articleId);
        articleFavoriteMapper.insert(favorite);
        notificationService.notifyFavorite(article, userId);
    }

    @Transactional
    public void unfavoriteArticle(Long userId, Long articleId) {
        int deleted = articleFavoriteMapper.delete(new QueryWrapper<ArticleFavorite>()
                .eq("user_id", userId)
                .eq("article_id", articleId));
        if (deleted == 0) {
            throw new RuntimeException("收藏记录不存在或不属于当前用户");
        }
    }

    public Page<Article> pageFavoriteArticles(Long userId, long pageNum, long pageSize) {
        Page<Article> page = new Page<>(pageNum, pageSize);
        Page<Article> result = articleMapper.selectFavoriteArticles(page, userId, STATUS_PUBLISHED);
        fillArticleMetadata(result.getRecords());
        return result;
    }

    public void update(Long userId, Long articleId, String title, String content) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        if (!article.getUserId().equals(userId)) {
            throw new RuntimeException("只能修改自己的文章");
        }
        if (!STATUS_DRAFT.equals(article.getStatus()) && !STATUS_REJECTED.equals(article.getStatus())) {
            throw new RuntimeException("只能修改草稿或被驳回文章");
        }
        article.setTitle(title);
        article.setContent(content);
        articleMapper.updateById(article);
    }

    @Transactional
    public void delete(Long userId, Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        if (!article.getUserId().equals(userId)) {
            throw new RuntimeException("只能删除自己的文章");
        }
        deleteArticleWithRelatedData(articleId);
    }

    @Transactional
    public boolean deleteByAdmin(Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            return false;
        }
        deleteArticleWithRelatedData(articleId);
        return true;
    }

    @Transactional
    public void approve(Long articleId, Long auditorId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        if (!STATUS_PENDING.equals(article.getStatus())) {
            throw new RuntimeException("只能审核待审核文章");
        }
        article.setStatus(STATUS_PUBLISHED);
        articleMapper.updateById(article);
        recordAudit(articleId, auditorId, STATUS_PUBLISHED, null);
        sendPublishMessage(article);
        notificationService.notifyAuditApproved(article, auditorId);
        log.info("文章审核通过，MQ 消息已发送");
    }

    @Transactional
    public void reject(Long articleId, Long auditorId, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new RuntimeException("驳回原因不能为空");
        }
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        if (!STATUS_PENDING.equals(article.getStatus())) {
            throw new RuntimeException("只能审核待审核文章");
        }
        article.setStatus(STATUS_REJECTED);
        articleMapper.updateById(article);
        recordAudit(articleId, auditorId, STATUS_REJECTED, reason);
        notificationService.notifyAuditRejected(article, auditorId, reason);
    }

    @Transactional
    public void offline(Long articleId, Long auditorId, String reason) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        if (!STATUS_PUBLISHED.equals(article.getStatus())) {
            throw new RuntimeException("只能下架已发布文章");
        }
        article.setStatus(STATUS_OFFLINE);
        articleMapper.updateById(article);
        recordAudit(articleId, auditorId, STATUS_OFFLINE, reason);
        redisService.deleteArticleData(articleId);
        notificationService.notifyArticleOffline(article, auditorId, reason);
    }

    private Article createArticle(String title, String content, Long categoryId, Long userId, String status) {
        Category category = categoryService.findEnabledById(categoryId);
        if (category == null) {
            throw new RuntimeException("板块不存在或已禁用");
        }

        Article article = new Article();
        article.setTitle(title);
        article.setContent(content);
        article.setCategoryId(categoryId);
        article.setUserId(userId);
        article.setStatus(status);
        articleMapper.insert(article);
        fillArticleMetadata(article);
        return article;
    }

    private Article findOwnedArticle(Long userId, Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        if (!article.getUserId().equals(userId)) {
            throw new RuntimeException("只能操作自己的文章");
        }
        return article;
    }

    private void deleteArticleWithRelatedData(Long articleId) {
        commentService.deleteByArticleId(articleId);
        articleFavoriteMapper.delete(new QueryWrapper<ArticleFavorite>().eq("article_id", articleId));
        articleMapper.deleteById(articleId);
        redisService.deleteArticleData(articleId);
    }

    private void sendPublishMessage(Article article) {
        ArticlePublishMessage articlePublishMessage = new ArticlePublishMessage();
        articlePublishMessage.setArticleId(article.getId());
        articlePublishMessage.setUserId(article.getUserId());
        articlePublishMessage.setTitle(article.getTitle());
        articlePublishMessage.setPublishTime(System.currentTimeMillis());
        articlePublishMessage.setMessageId(UUID.randomUUID().toString());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ARTICLE_EXCHANGE,
                RabbitMQConfig.ARTICLE_PUBLISH_ROUTING_KEY,
                articlePublishMessage);
    }

    private void recordAudit(Long articleId, Long auditorId, String auditStatus, String reason) {
        ArticleAuditRecord record = new ArticleAuditRecord();
        record.setArticleId(articleId);
        record.setAuditorId(auditorId);
        record.setAuditStatus(auditStatus);
        record.setReason(reason);
        articleAuditRecordMapper.insert(record);
    }

    private void validateStatus(String status) {
        if (!STATUS_DRAFT.equals(status)
                && !STATUS_PENDING.equals(status)
                && !STATUS_PUBLISHED.equals(status)
                && !STATUS_REJECTED.equals(status)
                && !STATUS_OFFLINE.equals(status)) {
            throw new RuntimeException("文章状态不正确");
        }
    }

    private void fillArticleMetadata(List<Article> articles) {
        if (articles == null || articles.isEmpty()) {
            return;
        }
        for (Article article : articles) {
            fillArticleMetadata(article);
        }
    }

    private void fillArticleMetadata(Article article) {
        if (article == null) {
            return;
        }
        fillCategoryName(article);
        fillFavoriteCount(article);
    }

    private void fillCategoryName(Article article) {
        if (article == null || article.getCategoryId() == null) {
            return;
        }
        Category category = categoryMapper.selectById(article.getCategoryId());
        if (category != null) {
            article.setCategoryName(category.getName());
        }
    }

    private void fillFavoriteCount(Article article) {
        if (article == null || article.getId() == null) {
            return;
        }
        Long count = articleFavoriteMapper.selectCount(
                new QueryWrapper<ArticleFavorite>().eq("article_id", article.getId()));
        article.setFavoriteCount(count);
    }
}
