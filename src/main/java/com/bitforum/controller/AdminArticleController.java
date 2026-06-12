package com.bitforum.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bitforum.common.Result;
import com.bitforum.entity.Article;
import com.bitforum.service.ArticleService;

@RestController
@RequestMapping("/api/admin/article")
public class AdminArticleController {
    @Autowired
    private ArticleService articleService;

    @GetMapping("/page")
    public Result<Page<Article>> pageArticles(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        // 管理端先复用普通文章分页能力；权限由 AdminInterceptor 在进入 Controller 前完成。
        Page<Article> articlePage = articleService.pageArticles(pageNum, pageSize);
        return Result.ok("管理员文章分页查询成功", articlePage);
    }

    @DeleteMapping("/delete")
    public Result<String> deleteArticle(@RequestParam Long articleId) {
        // 管理员删除不校验作者归属，但仍然必须复用统一清理流程，避免评论和 Redis 留下脏数据。
        boolean deleted = articleService.deleteByAdmin(articleId);
        if (!deleted) {
            return Result.fail("文章不存在");
        }
        return Result.ok("管理员删除文章成功", null);
    }
}
