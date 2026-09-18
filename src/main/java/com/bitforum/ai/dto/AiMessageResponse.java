package com.bitforum.ai.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "AI 会话消息")
public class AiMessageResponse {
    @Schema(description = "消息 ID")
    private Long id;
    @Schema(description = "消息角色：user / assistant / system / tool")
    private String role;
    @Schema(description = "消息内容")
    private String content;
    @Schema(description = "该条消息消耗的总 Token")
    private Integer totalTokens;
    @Schema(description = "处理耗时（毫秒）")
    private Integer latencyMs;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
