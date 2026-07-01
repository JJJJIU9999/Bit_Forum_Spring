package com.bitforum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "文章草稿保存请求")
public class ArticleDraftRequest {
    @NotBlank(message = "标题不能为空")
    @Size(max = 50, message = "标题不能超过50个字符")
    @Schema(description = "草稿标题，最多 50 个字符", example = "待完善的文章")
    private String title;

    @NotBlank(message = "内容不能为空")
    @Schema(description = "草稿正文", example = "先保存为草稿的正文内容")
    private String content;

    @Size(max = 255, message = "封面URL不能超过255个字符")
    @Schema(description = "文章封面 URL，可由上传接口返回", example = "/uploads/article-cover/550e8400-e29b-41d4-a716-446655440000.jpg")
    private String coverUrl;

    @NotNull(message = "板块不能为空")
    @Schema(description = "所属板块 ID", example = "1")
    private Long categoryId;
}
