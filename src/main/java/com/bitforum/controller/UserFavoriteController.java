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
@RequestMapping("/api/user/favorites")
public class UserFavoriteController {
    @Autowired
    private ArticleService articleService;

    @GetMapping
    public Result<Page<Article>> pageMyFavorites(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        Page<Article> articlePage = articleService.pageFavoriteArticles(userId, pageNum, pageSize);
        return Result.ok("我的收藏分页查询成功", articlePage);
    }
}
