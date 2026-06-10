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

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;



@RestController
@RequestMapping("/api/comment")
public class CommentController {
    @Autowired
    private CommentService commentService;

    @PostMapping("/publish")
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
    public Result<List<Comment>> listByArticle(@RequestParam Long articleId) {
        List<Comment> commentList = commentService.listByArticleId(articleId);
        return Result.ok("查询成功", commentList);
    }
    
}
