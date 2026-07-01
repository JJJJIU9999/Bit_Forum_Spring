package com.bitforum.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.bitforum.entity.Article;
import com.bitforum.entity.Category;
import com.bitforum.mapper.ArticleMapper;
import com.bitforum.mapper.CategoryMapper;

@SpringBootTest
@Transactional
class ArticleMetricSyncServiceTest {
    @Autowired
    private ArticleMetricSyncService articleMetricSyncService;
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @MockitoBean
    private RedisService redisService;

    @Test
    void syncShouldPersistRedisViewAndLikeCountsToArticleTable() {
        Article article = createPublishedArticle(0, 0);
        when(redisService.getViewsIfPresent(article.getId())).thenReturn(12L);
        when(redisService.getLikeCountIfPresent(article.getId())).thenReturn(3L);

        int updatedCount = articleMetricSyncService.syncArticleMetrics();

        Article saved = articleMapper.selectById(article.getId());
        assertEquals(1, updatedCount);
        assertEquals(12, saved.getViewCount());
        assertEquals(3, saved.getLikeCount());
    }

    @Test
    void syncShouldKeepDatabaseCountsWhenRedisHasNoMetricData() {
        Article article = createPublishedArticle(7, 4);
        when(redisService.getViewsIfPresent(article.getId())).thenReturn(null);
        when(redisService.getLikeCountIfPresent(article.getId())).thenReturn(null);

        int updatedCount = articleMetricSyncService.syncArticleMetrics();

        Article saved = articleMapper.selectById(article.getId());
        assertEquals(0, updatedCount);
        assertEquals(7, saved.getViewCount());
        assertEquals(4, saved.getLikeCount());
    }

    @Test
    void syncShouldNotFailWhenRedisThrowsException() {
        Article article = createPublishedArticle(5, 2);
        when(redisService.getViewsIfPresent(article.getId())).thenThrow(new RuntimeException("redis unavailable"));

        assertDoesNotThrow(() -> articleMetricSyncService.syncArticleMetrics());

        Article saved = articleMapper.selectById(article.getId());
        assertEquals(5, saved.getViewCount());
        assertEquals(2, saved.getLikeCount());
    }

    private Article createPublishedArticle(int viewCount, int likeCount) {
        Category category = categoryMapper.selectList(null).get(0);
        Article article = new Article();
        article.setTitle("M11-" + UUID.randomUUID());
        article.setContent("M11 Redis metric sync test article");
        article.setUserId(81101L);
        article.setCategoryId(category.getId());
        article.setStatus(ArticleService.STATUS_PUBLISHED);
        article.setViewCount(viewCount);
        article.setLikeCount(likeCount);
        articleMapper.insert(article);
        return article;
    }
}
