package com.bitforum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentPublishRequest {
    // 发布评论只允许前端传文章 id 和评论内容，用户 id 仍然来自登录态
    @NotNull(message = "文章ID不能为空")
    private Long articleId;

    @NotBlank(message = "评论区内容不能为空")
    private String content;
}
