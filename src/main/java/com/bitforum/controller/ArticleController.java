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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/article")
@Tag(name = "文章", description = "文章草稿、提交审核、公开查询、搜索、收藏、点赞和热门文章")
public class ArticleController {
    @Autowired
    private ArticleService articleService;

    @Autowired
    private RedisService redisService;
    @Autowired
    private NotificationService notificationService;

    @PostMapping("/publish")
    @Operation(summary = "提交文章审核", description = "需要登录。保留 publish 路径，当前语义为提交审核", security = @SecurityRequirement(name = "bearerAuth"))
    public Result<String> publish(
            @Valid @RequestBody ArticlePublishRequest request,
            @RequestAttribute("userId") Long userId) {
        try {
            articleService.publish(
                    request.getTitle(),
                    request.getContent(),
                    request.getCategoryId(),
                    userId,
                    request.getCoverUrl());
            return Result.ok("提交审核成功", null);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/draft")
    @Operation(summary = "保存文章草稿", description = "需要登录，草稿不会进入公开列表", security = @SecurityRequirement(name = "bearerAuth"))
    public Result<Article> saveDraft(
            @Valid @RequestBody ArticleDraftRequest request,
            @RequestAttribute("userId") Long userId) {
        try {
            Article article = articleService.saveDraft(
                    request.getTitle(),
                    request.getContent(),
                    request.getCategoryId(),
                    userId,
                    request.getCoverUrl());
            return Result.ok("草稿保存成功", article);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping("/draft")
    @Operation(summary = "更新文章草稿", description = "需要登录，仅作者可更新草稿或被驳回文章", security = @SecurityRequirement(name = "bearerAuth"))
    public Result<String> updateDraft(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody ArticleDraftUpdateRequest request) {
        try {
            articleService.updateDraft(
                    userId,
                    request.getArticleId(),
                    request.getTitle(),
                    request.getContent(),
                    request.getCategoryId(),
                    request.getCoverUrl());
            return Result.ok("草稿更新成功", null);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/submit")
    @Operation(summary = "提交草稿审核", description = "需要登录，将草稿或被驳回文章提交为待审核", security = @SecurityRequirement(name = "bearerAuth"))
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
    @Operation(summary = "收藏文章", description = "需要登录，只能收藏已发布文章", security = @SecurityRequirement(name = "bearerAuth"))
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
    @Operation(summary = "取消收藏文章", description = "需要登录，只能取消自己的收藏", security = @SecurityRequirement(name = "bearerAuth"))
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
    @Operation(summary = "修改文章", description = "需要登录，仅作者可修改草稿或被驳回文章", security = @SecurityRequirement(name = "bearerAuth"))
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
            articleService.update(
                    userId,
                    request.getArticleId(),
                    request.getTitle(),
                    request.getContent(),
                    request.getCoverUrl());
            return Result.ok("更改文章成功", null);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除文章", description = "需要登录，仅作者可删除自己的文章", security = @SecurityRequirement(name = "bearerAuth"))
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
    @Operation(summary = "查询全部公开文章", description = "公开接口，返回已发布文章")
    public Result<List<Article>> listAll() {
        List<Article> articleList = articleService.listAll();
        if (articleList == null) {
            return Result.fail("获取数据失败");
        }
        return Result.ok("获取的数据如下", articleList);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询公开文章", description = "公开接口，可按板块筛选，仅返回已发布文章")
    public Result<Page<Article>> pageArticles(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long categoryId) {
        Page<Article> articlePage = articleService.pageArticles(pageNum, pageSize, categoryId);
        return Result.ok("分页查询成功", articlePage);
    }

    @GetMapping("/search")
    @Operation(summary = "搜索公开文章", description = "公开接口，按标题或正文关键词搜索已发布文章")
    public Result<Page<Article>> searchArticles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        Page<Article> articlePage = articleService.searchPublishedArticles(keyword, categoryId, pageNum, pageSize);
        return Result.ok("搜索成功", articlePage);
    }

    @GetMapping("/detail")
    @Operation(summary = "查询文章详情", description = "公开接口，仅可查询已发布文章")
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
    @Operation(summary = "增加文章浏览量", description = "公开接口，仅对已发布文章增加浏览量")
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
    @Operation(summary = "点赞文章", description = "需要登录，只能点赞他人的已发布文章", security = @SecurityRequirement(name = "bearerAuth"))
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
    @Operation(summary = "取消点赞文章", description = "需要登录，取消当前用户对文章的点赞", security = @SecurityRequirement(name = "bearerAuth"))
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
    @Operation(summary = "查询热门文章", description = "公开接口，基于 Redis 热度并过滤为已发布文章")
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
