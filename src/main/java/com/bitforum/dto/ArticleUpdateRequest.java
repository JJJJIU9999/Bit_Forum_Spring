package com.bitforum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ArticleUpdateRequest {
    // 更新文章接口的入参 DTO，把原来散落在 URL 上的参数收拢到 JSON 请求体里
    @NotNull(message = "文章ID不能为空")
    private Long articleId;

    @NotBlank(message = "标题不能为空")
    @Size(max = 50,message = "标题不能超过50个字符")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;
}
