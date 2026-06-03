package com.bitforum.controller;


import java.util.ArrayList;
import java.util.List;
// import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bitforum.common.HotArticle;
import com.bitforum.common.Result;
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
    public Result<String> publish(@Valid @RequestBody Article article,
            @RequestAttribute("userId") Long userId) {
        articleService.publish(article.getTitle(), article.getContent(), userId);
        return Result.ok("发布成功", null);
    }
    
    //更新文章
    @PutMapping("/update")
    public Result<String> update(@RequestAttribute("userId") Long userId,@RequestParam Long articleId,@RequestParam String newTitle,@RequestParam String newContent){
        Article article = articleService.findById(articleId);
        if (article == null) {
            return Result.fail("文章不存在");
        }
        if (!article.getUserId().equals(userId)) {
            return Result.fail("只能修改自己的文章");
        }
        articleService.update(userId, articleId, newTitle, newContent);
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
        Long views = redisService.increaseViews(articleId);
        return Result.ok("浏览量+1，当前浏览量：" + views, null);
    }

    //点赞操作，把articleId和userId传入like操作，再返回点赞量likeCount
    @PostMapping("/like")
    public Result<String> like(@RequestParam Long articleId, @RequestAttribute("userId") Long userId) 
    {
        redisService.like(articleId, userId);
        Long count = redisService.getLikeCount(articleId);
        return Result.ok("点赞成功，当前点赞数量：" + count, null);
    }

    //取消点赞操作，把like方法换成unlike方法，其余的逻辑一致
    @PostMapping("/unlike")
    public Result<String> unlike(@RequestParam Long articleId, @RequestAttribute("userId") Long userId) 
    {
        redisService.unlike(articleId, userId);
        Long count = redisService.getLikeCount(articleId);
        return Result.ok("取消点赞成功，当前点赞数量：" + count, null);
    }



    @GetMapping("/hot")
    public Result<List<HotArticle>> hotList() {
        List<Article> allArticles = articleService.listAll();
        List<HotArticle> hotList = new ArrayList<>();

        for (Article article : allArticles) {
            long views = redisService.getViews(article.getId());
            long likes = redisService.getLikeCount(article.getId());
            long hotScore = views + likes * 3; //权重公式，1个赞 = 3次浏览
            article.setViewCount((int) views);
            article.setLikeCount((int) likes);
            hotList.add(new HotArticle(article, hotScore));
        }
        hotList.sort((a, b) -> Long.compare(b.getHotScore(), a.getHotScore()));
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
