package com.bitforum.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bitforum.common.HotArticle;
import com.bitforum.common.Result;
import com.bitforum.dto.ArticleDraftRequest;
import com.bitforum.dto.ArticleDraftUpdateRequest;
import com.bitforum.dto.ArticlePublishRequest;
import com.bitforum.dto.ArticleUpdateRequest;
import com.bitforum.entity.Article;
import com.bitforum.service.ArticleService;
import com.bitforum.service.NotificationService;
import com.bitforum.service.RedisService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/article")
public class ArticleController {
    @Autowired
    private ArticleService articleService;

    @Autowired
    private RedisService redisService;
    @Autowired
    private NotificationService notificationService;

    @PostMapping("/publish")
    public Result<String> publish(
            @Valid @RequestBody ArticlePublishRequest request,
            @RequestAttribute("userId") Long userId) {
        try {
            articleService.publish(request.getTitle(), request.getContent(), request.getCategoryId(), userId);
            return Result.ok("提交审核成功", null);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/draft")
    public Result<Article> saveDraft(
            @Valid @RequestBody ArticleDraftRequest request,
            @RequestAttribute("userId") Long userId) {
        try {
            Article article = articleService.saveDraft(
                    request.getTitle(),
                    request.getContent(),
                    request.getCategoryId(),
                    userId);
            return Result.ok("草稿保存成功", article);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping("/draft")
    public Result<String> updateDraft(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody ArticleDraftUpdateRequest request) {
        try {
            articleService.updateDraft(
                    userId,
                    request.getArticleId(),
                    request.getTitle(),
                    request.getContent(),
                    request.getCategoryId());
            return Result.ok("草稿更新成功", null);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/submit")
    public Result<String> submit(
            @RequestAttribute("userId") Long userId,
            @RequestParam Long articleId) {
        try {
            articleService.submit(userId, articleId);
            return Result.ok("提交审核成功", null);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/favorite")
    public Result<String> favorite(
            @RequestParam Long articleId,
            @RequestAttribute("userId") Long userId) {
        try {
            articleService.favoriteArticle(userId, articleId);
            return Result.ok("收藏成功", null);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @DeleteMapping("/favorite")
    public Result<String> unfavorite(
            @RequestParam Long articleId,
            @RequestAttribute("userId") Long userId) {
        try {
            articleService.unfavoriteArticle(userId, articleId);
            return Result.ok("取消收藏成功", null);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping("/update")
    public Result<String> update(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody ArticleUpdateRequest request) {
        try {
            Article article = articleService.findById(request.getArticleId());
            if (article == null) {
                return Result.fail("文章不存在");
            }
            if (!article.getUserId().equals(userId)) {
                return Result.fail("只能修改自己的文章");
            }
            articleService.update(userId, request.getArticleId(), request.getTitle(), request.getContent());
            return Result.ok("更改文章成功", null);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    public Result<String> delete(
            @RequestAttribute("userId") Long userId,
            @RequestParam Long articleId) {
        try {
            Article article = articleService.findById(articleId);
            if (article == null) {
                return Result.fail("删除失败，没有此文章");
            }
            if (!article.getUserId().equals(userId)) {
                return Result.fail("只能删除自己的文章");
            }
            articleService.delete(userId, articleId);
            return Result.ok("删除成功", null);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/listAll")
    public Result<List<Article>> listAll() {
        List<Article> articleList = articleService.listAll();
        if (articleList == null) {
            return Result.fail("获取数据失败");
        }
        return Result.ok("获取的数据如下", articleList);
    }

    @GetMapping("/page")
    public Result<Page<Article>> pageArticles(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long categoryId) {
        Page<Article> articlePage = articleService.pageArticles(pageNum, pageSize, categoryId);
        return Result.ok("分页查询成功", articlePage);
    }

    @GetMapping("/search")
    public Result<Page<Article>> searchArticles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        Page<Article> articlePage = articleService.searchPublishedArticles(keyword, categoryId, pageNum, pageSize);
        return Result.ok("搜索成功", articlePage);
    }

    @GetMapping("/detail")
    public Result<Article> detail(@RequestParam Long articleId) {
        Article article = articleService.findPublishedById(articleId);
        if (article == null) {
            return Result.fail("文章不存在");
        }
        Long views = redisService.getViews(articleId);
        article.setViewCount(views.intValue());
        article.setLikeCount(redisService.getLikeCount(articleId).intValue());
        return Result.ok("文章查找成功", article);
    }

    @GetMapping("/view")
    public Result<String> view(@RequestParam Long articleId) {
        Article article = articleService.findPublishedById(articleId);
        if (article == null) {
            return Result.fail("文章不存在");
        }
        Long views = redisService.increaseViews(articleId);
        redisService.increaseHot(articleId, 1);
        return Result.ok("浏览量+1，当前浏览量：" + views, null);
    }

    @PostMapping("/like")
    public Result<String> like(
            @RequestParam Long articleId,
            @RequestAttribute("userId") Long userId) {
        Article article = articleService.findPublishedById(articleId);
        if (article == null) {
            return Result.fail("文章不存在");
        }
        if (userId.equals(article.getUserId())) {
            return Result.fail("不能给自己的文章点赞");
        }
        boolean liked = redisService.like(articleId, userId);
        if (liked) {
            redisService.increaseHot(articleId, 3);
            notificationService.notifyLike(article, userId);
        }
        Long count = redisService.getLikeCount(articleId);
        String message = liked ? "点赞成功，当前点赞数量：" + count : "已经点赞过，当前点赞数量：" + count;
        return Result.ok(message, null);
    }

    @PostMapping("/unlike")
    public Result<String> unlike(
            @RequestParam Long articleId,
            @RequestAttribute("userId") Long userId) {
        Article article = articleService.findPublishedById(articleId);
        if (article == null) {
            return Result.fail("文章不存在");
        }
        redisService.unlike(articleId, userId);
        Long count = redisService.getLikeCount(articleId);
        return Result.ok("取消点赞成功，当前点赞数量：" + count, null);
    }

    @GetMapping("/hot")
    public Result<List<HotArticle>> hotList() {
        Set<ZSetOperations.TypedTuple<String>> hotSet = redisService.getHotList(10);
        List<HotArticle> hotList = new ArrayList<>();

        if (hotSet == null || hotSet.isEmpty()) {
            return Result.ok("热门文章如下", hotList);
        }

        for (ZSetOperations.TypedTuple<String> tuple : hotSet) {
            Long articleId = Long.parseLong(tuple.getValue());
            Article article = articleService.findPublishedById(articleId);

            if (article != null) {
                article.setViewCount(redisService.getViews(articleId).intValue());
                article.setLikeCount(redisService.getLikeCount(articleId).intValue());

                Long hotScore = tuple.getScore() == null ? 0L : tuple.getScore().longValue();
                hotList.add(new HotArticle(article, hotScore));
            }
        }
        return Result.ok("热门文章如下", hotList);
    }
}
