package com.bitforum.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bitforum.common.Result;
import com.bitforum.entity.Comment;
import com.bitforum.service.CommentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.GetMapping;



@RestController
@RequestMapping("/api/comment")
public class CommentController {
    @Autowired
    private CommentService commentService;

    @PostMapping("/publish")
    public Result<String> publish(
            @RequestAttribute("userId") Long userId,
            @RequestParam Long articleId,
            @RequestParam String content) {
        commentService.publish(userId, articleId, content);
        return Result.ok("评论发布成功", null);
    }
        
    @GetMapping("/listAll")
    public Result<List<Comment>> listByArticle(@RequestParam Long articleId) {
        List<Comment> commentList = commentService.listByArticleId(articleId);
        return Result.ok("查询成功", commentList);
    }
    
}
