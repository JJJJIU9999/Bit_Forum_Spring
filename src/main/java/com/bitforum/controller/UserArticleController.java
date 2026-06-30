package com.bitforum.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bitforum.common.Result;
import com.bitforum.entity.Article;
import com.bitforum.service.ArticleService;

@RestController
@RequestMapping("/api/user/articles")
public class UserArticleController {
    @Autowired
    private ArticleService articleService;

    @GetMapping
    public Result<Page<Article>> pageMyArticles(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String status) {
        try {
            Page<Article> articlePage = articleService.pageUserArticles(userId, pageNum, pageSize, status);
            return Result.ok("我的文章分页查询成功", articlePage);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }
}
