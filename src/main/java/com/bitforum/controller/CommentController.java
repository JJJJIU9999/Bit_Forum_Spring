package com.bitforum.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bitforum.common.Result;
import com.bitforum.dto.CommentPublishRequest;
import com.bitforum.entity.Comment;
import com.bitforum.service.CommentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;



@RestController
@RequestMapping("/api/comment")
@Tag(name = "评论", description = "文章评论发布和查询")
public class CommentController {
    @Autowired
    private CommentService commentService;

    @PostMapping("/publish")
    @Operation(summary = "发表评论", description = "需要登录，只能评论已发布文章", security = @SecurityRequirement(name = "bearerAuth"))
    public Result<String> publish(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody CommentPublishRequest request) {
        // Service 负责校验文章是否存在；Controller 根据结果返回接口响应
        // userId 来自登录拦截器，articleId 和 content 来自评论发布 DTO
        boolean success = commentService.publish(userId, request.getArticleId(),request.getContent());
        if (!success) {
            return Result.fail("文章不存在");
        }
        return Result.ok("评论发布成功", null);
    }
        
    @GetMapping("/listAll")
    @Operation(summary = "查询文章评论", description = "公开接口，按文章 ID 查询评论列表")
    public Result<List<Comment>> listByArticle(@RequestParam Long articleId) {
        List<Comment> commentList = commentService.listByArticleId(articleId);
        return Result.ok("查询成功", commentList);
    }
    
}
