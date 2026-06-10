package com.bitforum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ArticlePublishRequest {
    // 发布文章时前端只需要传标题和内容，不让前端控制 id、userId、浏览量等数据库字段
    @NotBlank(message = "标题不能为空")
    @Size(max = 50,message = "标题不能超过50个字符")
    private String title;

    @NotBlank(message="内容不能为空")
    private String content;
}
