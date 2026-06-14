package com.bitforum.controller;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bitforum.common.HotArticle;
import com.bitforum.common.Result;
import com.bitforum.dto.ArticlePublishRequest;
import com.bitforum.dto.ArticleUpdateRequest;
import com.bitforum.entity.Article;
import com.bitforum.service.ArticleService;
import com.bitforum.service.RedisService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/article")

public class ArticleController {
    @Autowired
    private ArticleService articleService;

    @Autowired
    private RedisService redisService;

    //发表文章
    @PostMapping("/publish")
    public Result<String> publish(@Valid @RequestBody ArticlePublishRequest request,
            @RequestAttribute("userId") Long userId) {
        // Controller 只接收发布文章需要的字段，真正的作者 id 来自登录拦截器写入的 userId
        articleService.publish(request.getTitle(), request.getContent(), userId);
        return Result.ok("发布成功", null);
    }

    //更新文章
    @PutMapping("/update")
    public Result<String> update(@RequestAttribute("userId") Long userId,@Valid @RequestBody ArticleUpdateRequest request){
        // 更新文章时用 Request DTO 统一接收 JSON，请求体里只放文章 id、标题和内容
        Article article = articleService.findById(request.getArticleId());
        if (article == null) {
            return Result.fail("文章不存在");
        }
        if (!article.getUserId().equals(userId)) {
            return Result.fail("只能修改自己的文章");
        }
        // 作者身份仍然以登录拦截器写入的 userId 为准，避免前端伪造作者 id
        articleService.update(userId, request.getArticleId(),request.getTitle(),request.getContent());
        return Result.ok("更改文章成功", null);
    }

    //删除文章
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestAttribute ("userId") Long userId,@RequestParam Long articleId) {
        Article article = articleService.findById(articleId);
        if (article == null) {
            return Result.fail("删除失败，没有此文章。");
        }
        if (!article.getUserId().equals(userId)) {
            return Result.fail("只能删除自己的文章");
        }
        articleService.delete(userId,articleId);
        return Result.ok("删除成功！", null);
    }

    //获取数据库中的所有文章数据
    @GetMapping("/listAll")
    public Result<List<Article>> listAll() {
        List<Article> articleList = articleService.listAll();
        if (articleList == null) {
            return Result.fail("获取数据失败！");
        }
        return Result.ok("获取的数据如下：", articleList);
    }

    @GetMapping("/page")
    public Result<Page<Article>> pageArticles(
        @RequestParam(defaultValue = "1") long pageNum,
        @RequestParam(defaultValue = "10") long pageSize) {
        // 不传参数时默认查第 1 页、每页 10 条，避免一次性返回全部文章
        Page<Article> articlePage = articleService.pageArticles(pageNum, pageSize);
        // Page 里包含 records、total、pages、current、size 等分页元信息
        return Result.ok("分页查询成功", articlePage);
    }

    //获取指定id的文章数据
    @GetMapping("/detail")
    public Result<Article> detail(@RequestParam Long articleId) {
        Article article = articleService.findById(articleId);
        if (article == null) {
            return Result.fail("文章不存在");
        }
        Long views = redisService.getViews(articleId);
        article.setViewCount(views.intValue()); // ← 把浏览量装进 Article 对象
        article.setLikeCount(redisService.getLikeCount(articleId).intValue());
        return Result.ok("文章查找成功！", article);
    }

    //获取浏览量viewCount，并且通过increaseViews方法使浏览量+1
    @GetMapping("/view")
    public Result<String> view(@RequestParam Long articleId) {
        // 先查 MySQL，确认文章真实存在；否则不能给不存在的文章创建 Redis 浏览量
        Article article = articleService.findById(articleId);
        if (article == null) {
            return Result.fail("文章不存在");
        }
        Long views = redisService.increaseViews(articleId);
        // 浏览一次文章，就给 Redis ZSet 热榜增加 1 分
        redisService.increaseHot(articleId,1);
        return Result.ok("浏览量+1，当前浏览量：" + views, null);
    }

    //点赞操作，把articleId和userId传入like操作，再返回点赞量likeCount
    @PostMapping("/like")
    public Result<String> like(@RequestParam Long articleId, @RequestAttribute("userId") Long userId)
    {
        // 点赞前先校验文章存在，避免 Redis 中出现 article:{不存在id}:likes
        Article article = articleService.findById(articleId);
        if (article == null) {
            return Result.fail("文章不存在");
        }
        boolean liked = redisService.like(articleId, userId);
        // 只有第一次点赞才给热榜加分；重复点赞不会改变 Redis Set，也不应该重复增加热度
        if (liked) {
            redisService.increaseHot(articleId,3);
        }
        Long count = redisService.getLikeCount(articleId);
        String message = liked ? "点赞成功，当前点赞数量：" + count : "已经点赞过，当前点赞数量：" + count;
        return Result.ok(message, null);
    }

    //取消点赞操作，把like方法换成unlike方法，其余的逻辑一致
    @PostMapping("/unlike")
    public Result<String> unlike(@RequestParam Long articleId, @RequestAttribute("userId") Long userId)
    {
        // 取消点赞也要校验文章存在，保持 MySQL 文章数据和 Redis 点赞数据一致
        Article article = articleService.findById(articleId);
        if (article == null) {
            return Result.fail("文章不存在");
        }
        redisService.unlike(articleId, userId);
        Long count = redisService.getLikeCount(articleId);
        return Result.ok("取消点赞成功，当前点赞数量：" + count, null);
    }



    @GetMapping("/hot")
    public Result<List<HotArticle>> hotList() {
        // 直接从 Redis ZSet 取热度最高的前 10 篇文章，避免每次遍历全部文章再排序
        // 一组 Redis ZSet 排行记录
        // 每条记录里都有：
        // - value：文章ID，String 类型
        // - score：热度分，Double 类型
        // ZSetOperations.TypedTuple<String>代表 Redis ZSet 里面的一条数据。
        Set<ZSetOperations.TypedTuple<String>> hotSet = redisService.getHotList(10);
        List<HotArticle> hotList = new ArrayList<>();

        if (hotSet == null || hotSet.isEmpty()) {
            return Result.ok("热门文章如下", hotList);
        }

        for (ZSetOperations.TypedTuple<String> tuple : hotSet) {
            // ZSet 的 value 存 articleId，score 存这篇文章当前的热度分
            //1. 从这条记录里拿到文章 ID
            Long articleId = Long.parseLong(tuple.getValue());
            // 2. 根据文章 ID 去 MySQL 查文章详情
            Article article = articleService.findById(articleId);

            if (article != null) {
                article.setViewCount(redisService.getViews(articleId).intValue());
                article.setLikeCount(redisService.getLikeCount(articleId).intValue());

                Long hotScore = tuple.getScore() == null ? 0L : tuple.getScore().longValue();
                hotList.add(new HotArticle(article, hotScore));
            }
        }
        return Result.ok("热门文章如下：", hotList);
    }




    //ZSet排序获取热门文章前十排行榜
    // @GetMapping("/hot")
    // public Result<List<Article>> hostList() {
    //     Set<ZSetOperations.TypedTuple<String>> hotSet = redisService.getHotList(10);
    //     List<Article> hotArticleList = new ArrayList<>();
    //     for (ZSetOperations.TypedTuple<String> tuple : hotSet) {
    //         Long articleId = Long.parseLong(tuple.getValue());
    //         Article article = articleService.findById(articleId);
    //         if (article != null) {
    //             article.setViewCount(redisService.getViews(articleId).intValue());
    //             article.setLikeCount(redisService.getLikeCount(articleId).intValue());
    //             hotArticleList.add(article);
    //         }
    //     }
    //     return Result.ok("热门文章如下", hotArticleList);
    // }


}
