package com.bitforum.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bitforum.common.Result;
import com.bitforum.dto.ArticleAuditRejectRequest;
import com.bitforum.dto.ArticleOfflineRequest;
import com.bitforum.entity.Article;
import com.bitforum.service.ArticleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/article")
public class AdminArticleController {
    @Autowired
    private ArticleService articleService;

    @GetMapping("/page")
    public Result<Page<Article>> pageArticles(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long categoryId) {
        Page<Article> articlePage = articleService.pageAdminArticles(pageNum, pageSize, categoryId);
        return Result.ok("管理员文章分页查询成功", articlePage);
    }

    @GetMapping("/audit/page")
    public Result<Page<Article>> pageAuditArticles(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        try {
            Page<Article> articlePage = articleService.pageAuditArticles(pageNum, pageSize, status);
            return Result.ok("审核文章分页查询成功", articlePage);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping("/audit/approve")
    public Result<String> approve(
            @RequestParam Long articleId,
            @RequestAttribute("userId") Long auditorId) {
        try {
            articleService.approve(articleId, auditorId);
            return Result.ok("审核通过成功", null);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping("/audit/reject")
    public Result<String> reject(
            @Valid @RequestBody ArticleAuditRejectRequest request,
            @RequestAttribute("userId") Long auditorId) {
        try {
            articleService.reject(request.getArticleId(), auditorId, request.getReason());
            return Result.ok("审核驳回成功", null);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping("/offline")
    public Result<String> offline(
            @Valid @RequestBody ArticleOfflineRequest request,
            @RequestAttribute("userId") Long auditorId) {
        try {
            articleService.offline(request.getArticleId(), auditorId, request.getReason());
            return Result.ok("文章下架成功", null);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    public Result<String> deleteArticle(@RequestParam Long articleId) {
        boolean deleted = articleService.deleteByAdmin(articleId);
        if (!deleted) {
            return Result.fail("文章不存在");
        }
        return Result.ok("管理员删除文章成功", null);
    }
}
