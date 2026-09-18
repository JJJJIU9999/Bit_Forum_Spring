package com.bitforum.ai.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.bitforum.ai.dto.AiChatRequest;
import com.bitforum.ai.dto.AiConversationCreateRequest;
import com.bitforum.ai.dto.AiConversationResponse;
import com.bitforum.ai.dto.AiMessageResponse;
import com.bitforum.ai.orchestrator.AgentOrchestrator;
import com.bitforum.ai.service.AiConversationService;
import com.bitforum.common.Result;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * AI 助手接口（M13）。
 *
 * 全部接口都要求登录：由 WebMvcConfig 中的 LoginInterceptor 拦截 /api/ai/**，
 * 因此这里可以直接通过 @RequestAttribute("userId") 拿到当前登录用户。
 */
@RestController
@Tag(name = "AI 助手", description = "AI 会话管理与多轮对话")
public class AiController {

    private final AiConversationService conversationService;
    private final AgentOrchestrator orchestrator;

    public AiController(AiConversationService conversationService, AgentOrchestrator orchestrator) {
        this.conversationService = conversationService;
        this.orchestrator = orchestrator;
    }

    @PostMapping("/api/ai/conversations")
    @Operation(summary = "新建 AI 会话", description = "为当前登录用户创建一个新的 AI 对话会话",
            security = @SecurityRequirement(name = "bearerAuth"))
    public Result<AiConversationResponse> createConversation(
            @Valid @RequestBody(required = false) AiConversationCreateRequest request,
            @RequestAttribute("userId") Long userId) {
        String title = request == null ? null : request.getTitle();
        return Result.ok("会话创建成功", conversationService.createConversation(userId, title));
    }

    @GetMapping("/api/ai/conversations")
    @Operation(summary = "查询我的 AI 会话列表", description = "按最后更新时间倒序返回当前登录用户的全部会话",
            security = @SecurityRequirement(name = "bearerAuth"))
    public Result<List<AiConversationResponse>> listConversations(
            @RequestAttribute("userId") Long userId) {
        return Result.ok("会话列表查询成功", conversationService.listConversations(userId));
    }

    @PostMapping("/api/ai/conversations/{conversationId}/messages")
    @Operation(summary = "发送消息给 AI 助手",
            description = "保存用户提问、调用大模型生成回答并返回。大模型不可用时返回降级回答，不影响论坛其他功能",
            security = @SecurityRequirement(name = "bearerAuth"))
    public Result<AiMessageResponse> sendMessage(
            @Parameter(description = "会话 ID") @PathVariable Long conversationId,
            @Valid @RequestBody AiChatRequest request,
            @RequestAttribute("userId") Long userId) {
        try {
            return Result.ok("AI 回答生成成功",
                    orchestrator.chat(conversationId, userId, request.getContent()));
        } catch (ResponseStatusException e) {
            // 会话不存在（404）与越权访问（403）交由 GlobalExceptionHandler 映射为对应 HTTP 状态，
            // 不能在这里吞成 200，否则前端无法区分"无权限"和"服务异常"。
            throw e;
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/api/ai/conversations/{conversationId}/messages")
    @Operation(summary = "查询会话消息历史", description = "按时间正序返回指定会话的全部消息",
            security = @SecurityRequirement(name = "bearerAuth"))
    public Result<List<AiMessageResponse>> listMessages(
            @Parameter(description = "会话 ID") @PathVariable Long conversationId,
            @RequestAttribute("userId") Long userId) {
        return Result.ok("消息历史查询成功",
                conversationService.listMessages(conversationId, userId));
    }
}
