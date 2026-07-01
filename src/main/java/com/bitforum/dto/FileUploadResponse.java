package com.bitforum.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "文件上传响应")
public class FileUploadResponse {
    @Schema(description = "前端可访问的文件 URL", example = "/uploads/avatar/550e8400-e29b-41d4-a716-446655440000.png")
    private String url;

    @Schema(description = "原始文件名", example = "avatar.png")
    private String originalFilename;

    @Schema(description = "文件大小，单位字节", example = "102400")
    private long size;

    @Schema(description = "文件 MIME 类型", example = "image/png")
    private String contentType;
}
