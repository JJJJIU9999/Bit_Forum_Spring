package com.bitforum.ai.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bitforum.ai.agent.AgentType;
import com.bitforum.ai.dto.AiConversationResponse;
import com.bitforum.ai.dto.AiMessageResponse;
import com.bitforum.ai.entity.AiConversation;
import com.bitforum.ai.entity.AiMessage;
import com.bitforum.ai.mapper.AiConversationMapper;
import com.bitforum.ai.mapper.AiMessageMapper;

/**
 * AI 会话与消息的业务服务（M13）。
 *
 * 职责边界：
 * - 本类只负责会话与消息的读写、归属校验和标题生成；
 * - 调用大模型的逻辑放在 Agent 与 AgentOrchestrator 中，保持职责单一。
 */
@Service
public class AiConversationService {

    /** 会话标题最大长度，与 ai_conversation.title 的 VARCHAR(100) 保持一致 */
    private static final int MAX_TITLE_LENGTH = 100;

    private final AiConversationMapper aiConversationMapper;
    private final AiMessageMapper aiMessageMapper;

    public AiConversationService(AiConversationMapper aiConversationMapper,
                                 AiMessageMapper aiMessageMapper) {
        this.aiConversationMapper = aiConversationMapper;
        this.aiMessageMapper = aiMessageMapper;
    }

    /**
     * 新建会话。
     *
     * @param userId 会话归属用户
     * @param title  会话标题，为空时使用默认标题
     */
    @Transactional
    public AiConversationResponse createConversation(Long userId, String title) {
        AiConversation conversation = new AiConversation();
        conversation.setUserId(userId);
        conversation.setTitle(normalizeTitle(title));
        conversation.setAgentType(AgentType.QA.name());
        conversation.setMessageCount(0);
        conversation.setTotalTokens(0);
        aiConversationMapper.insert(conversation);
        return toResponse(conversation);
    }

    /** 查询当前用户的会话列表，按更新时间倒序。 */
    public List<AiConversationResponse> listConversations(Long userId) {
        List<AiConversation> conversations = aiConversationMapper.selectList(
                new LambdaQueryWrapper<AiConversation>()
                        .eq(AiConversation::getUserId, userId)
                        .orderByDesc(AiConversation::getUpdateTime)
                        .orderByDesc(AiConversation::getId));
        return conversations.stream().map(this::toResponse).toList();
    }

    /**
     * 查询会话详情并校验归属，防止越权读取他人会话。
     *
     * @throws ResponseStatusException 会话不存在（404）或不属于该用户（403）
     */
    public AiConversation getOwnedConversation(Long conversationId, Long userId) {
        AiConversation conversation = aiConversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "会话不存在");
        }
        if (!conversation.getUserId().equals(userId)) {
            // 与项目既有权限约定一致：越权访问返回 403
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权访问该会话");
        }
        return conversation;
    }

    /** 查询会话内的全部消息，按创建顺序正序。 */
    public List<AiMessageResponse> listMessages(Long conversationId, Long userId) {
        getOwnedConversation(conversationId, userId);
        List<AiMessage> messages = aiMessageMapper.selectList(new LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getConversationId, conversationId)
                .orderByAsc(AiMessage::getId));
        return messages.stream().map(this::toResponse).toList();
    }

    /** 保存一条消息，并同步维护会话的消息数与更新时间。 */
    @Transactional
    public AiMessage saveMessage(Long conversationId, String role, String content,
                                 Integer promptTokens, Integer completionTokens,
                                 Integer totalTokens, Integer latencyMs) {
        AiMessage message = new AiMessage();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content == null ? "" : content);
        message.setPromptTokens(promptTokens);
        message.setCompletionTokens(completionTokens);
        message.setTotalTokens(totalTokens);
        message.setLatencyMs(latencyMs);
        aiMessageMapper.insert(message);
        return message;
    }

    /** 会话产生新消息后更新统计字段。 */
    @Transactional
    public void updateConversationStats(Long conversationId, int addedMessages, int addedTokens) {
        AiConversation conversation = aiConversationMapper.selectById(conversationId);
        if (conversation == null) {
            return;
        }
        int messageCount = conversation.getMessageCount() == null ? 0 : conversation.getMessageCount();
        int totalTokens = conversation.getTotalTokens() == null ? 0 : conversation.getTotalTokens();
        conversation.setMessageCount(messageCount + addedMessages);
        conversation.setTotalTokens(totalTokens + addedTokens);
        aiConversationMapper.updateById(conversation);
    }

    /**
     * 用首条用户消息生成会话标题。
     * 只在会话标题仍为默认值时调用，避免覆盖用户自定义标题。
     */
    @Transactional
    public void renameIfDefault(Long conversationId, String firstUserMessage) {
        AiConversation conversation = aiConversationMapper.selectById(conversationId);
        if (conversation == null || firstUserMessage == null || firstUserMessage.isBlank()) {
            return;
        }
        if (!"新对话".equals(conversation.getTitle())) {
            return;
        }
        conversation.setTitle(truncate(firstUserMessage.trim()));
        aiConversationMapper.updateById(conversation);
    }

    // ==================== 内部方法 ====================

    private String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return "新对话";
        }
        return truncate(title.trim());
    }

    private String truncate(String text) {
        if (text.length() <= MAX_TITLE_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_TITLE_LENGTH);
    }

    private AiConversationResponse toResponse(AiConversation conversation) {
        AiConversationResponse response = new AiConversationResponse();
        response.setId(conversation.getId());
        response.setTitle(conversation.getTitle());
        response.setAgentType(conversation.getAgentType());
        response.setMessageCount(conversation.getMessageCount());
        response.setTotalTokens(conversation.getTotalTokens());
        response.setCreateTime(conversation.getCreateTime());
        response.setUpdateTime(conversation.getUpdateTime());
        return response;
    }

    private AiMessageResponse toResponse(AiMessage message) {
        AiMessageResponse response = new AiMessageResponse();
        response.setId(message.getId());
        response.setRole(message.getRole());
        response.setContent(message.getContent());
        response.setTotalTokens(message.getTotalTokens());
        response.setLatencyMs(message.getLatencyMs());
        response.setCreateTime(message.getCreateTime());
        return response;
    }
}
