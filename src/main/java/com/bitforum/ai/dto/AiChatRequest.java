package com.bitforum.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "向 AI 助手发送消息请求")
public class AiChatRequest {
    // 用户 id 一律来自登录态（LoginInterceptor 解析 JWT），不接受前端传入
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "单条消息不能超过 2000 个字符")
    @Schema(description = "用户提问内容")
    private String content;
}
