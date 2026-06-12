package com.bitforum.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bitforum.common.Result;
import com.bitforum.entity.Comment;
import com.bitforum.service.CommentService;

@RestController
@RequestMapping("/api/admin/comment")
public class AdminCommentController {
    @Autowired
    private CommentService commentService;

    @GetMapping("/page")
    public Result<Page<Comment>> pageComments(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        // 权限由 AdminInterceptor 统一处理，这里只负责返回管理端需要的评论分页数据。
        Page<Comment> commentPage = commentService.pageComments(pageNum, pageSize);
        return Result.ok("管理员评论分页查询成功", commentPage);
    }

    @DeleteMapping("/delete")
    public Result<String> deleteComment(@RequestParam Long commentId) {
        // 管理员可以删除任意评论；Service 用 boolean 区分“已删除”和“评论不存在”。
        boolean deleted = commentService.deleteByAdmin(commentId);
        if (!deleted) {
            return Result.fail("评论不存在");
        }
        return Result.ok("管理员删除评论成功", null);
    }
}
