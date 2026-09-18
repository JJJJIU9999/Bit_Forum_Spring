package com.bitforum.ai.orchestrator;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import com.bitforum.ai.agent.Agent;
import com.bitforum.ai.agent.AgentContext;
import com.bitforum.ai.agent.AgentResponse;
import com.bitforum.ai.agent.AgentType;
import com.bitforum.ai.dto.AiMessageResponse;
import com.bitforum.ai.entity.AiConversation;
import com.bitforum.ai.memory.MysqlChatMemoryRepository;
import com.bitforum.ai.service.AiConversationService;

/**
 * Agent 编排器（M13）。
 *
 * 职责：加载会话历史 → 路由到对应 Agent → 持久化用户消息与助手回复 → 更新会话统计。
 *
 * M13 只有 QA 一个 Agent，路由表按类型装配，M16-M17 增加 Agent 时无需改动本类结构。
 */
@Service
public class AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);

    private final Map<AgentType, Agent> agentsByType;
    private final AiConversationService conversationService;
    private final MysqlChatMemoryRepository chatMemoryRepository;

    public AgentOrchestrator(List<Agent> agents,
                             AiConversationService conversationService,
                             MysqlChatMemoryRepository chatMemoryRepository) {
        this.conversationService = conversationService;
        this.chatMemoryRepository = chatMemoryRepository;
        // 过滤掉 type() 返回 null 的实现：null 作为 map key 会让该 Agent 永久无法被路由，
        // 且不会有任何报错，属于静默失效。这里显式跳过并记录警告，便于尽早发现实现错误。
        Map<AgentType, Agent> registry = new EnumMap<>(AgentType.class);
        for (Agent agent : agents) {
            AgentType type = agent.type();
            if (type == null) {
                log.warn("Agent 实现的 type() 返回 null，已跳过注册：{}", agent.getClass().getName());
                continue;
            }
            Agent previous = registry.put(type, agent);
            if (previous != null) {
                log.warn("存在重复的 Agent 类型 {}，后注册的实现将覆盖前者：{} -> {}",
                        type, previous.getClass().getName(), agent.getClass().getName());
            }
        }
        this.agentsByType = registry;
    }

    /**
     * 处理一轮对话：保存用户提问、生成回答、保存回答。
     *
     * @param conversationId 会话 id
     * @param userId         当前登录用户 id（用于归属校验）
     * @param userMessage    用户输入
     * @return 助手回复（已落库）
     */
    public AiMessageResponse chat(Long conversationId, Long userId, String userMessage) {
        // 1. 会话存在性与归属校验；越权直接抛 403/404
        AiConversation conversation = conversationService.getOwnedConversation(conversationId, userId);

        // 2. 先落库用户消息，保证即使后续模型调用失败，用户提问也不会丢
        conversationService.saveMessage(conversationId, "user", userMessage, null, null, null, null);

        // 3. 判断是否首轮对话：是则用首条提问生成会话标题
        int messageCount = conversation.getMessageCount() == null ? 0 : conversation.getMessageCount();
        if (messageCount == 0) {
            conversationService.renameIfDefault(conversationId, userMessage);
        }

        // 4. 加载完整历史（含刚保存的这条），与 Spring AI 的会话记忆共用同一数据源
        List<Message> history = new ArrayList<>(
                chatMemoryRepository.findByConversationId(String.valueOf(conversationId)));

        // 5. 分离出"本次输入"和"之前的历史"，避免把当前提问重复发送给模型
        String currentInput = extractAndRemoveLastUserMessage(history, userMessage);

        // 6. 路由到对应 Agent 执行
        AgentType agentType = AgentType.fromName(conversation.getAgentType());
        Agent agent = agentsByType.get(agentType);
        if (agent == null) {
            log.warn("未找到 Agent 实现：type={}，回退到 QA", agentType);
            agent = agentsByType.get(AgentType.QA);
        }
        if (agent == null) {
            // 理论上不会发生（QaAgent 始终注册）；防御性处理，避免 NPE
            throw new IllegalStateException("没有任何可用的 Agent 实现");
        }

        long startedAt = System.currentTimeMillis();
        AgentResponse response = agent.execute(
                new AgentContext(userId, conversationId, currentInput), history);
        int latencyMs = (int) (System.currentTimeMillis() - startedAt);

        // 7. 保存助手回复并更新会话统计
        var saved = conversationService.saveMessage(conversationId, "assistant", response.content(),
                response.promptTokens(), response.completionTokens(), response.totalTokens(), latencyMs);
        if (response.degraded()) {
            log.warn("本轮对话走了降级链路，conversationId={}", conversationId);
        }
        conversationService.updateConversationStats(conversationId, 2,
                response.totalTokens() == null ? 0 : response.totalTokens());

        AiMessageResponse result = new AiMessageResponse();
        result.setId(saved.getId());
        result.setRole(saved.getRole());
        result.setContent(saved.getContent());
        result.setTotalTokens(saved.getTotalTokens());
        result.setLatencyMs(saved.getLatencyMs());
        result.setCreateTime(saved.getCreateTime());
        return result;
    }

    /**
     * 从历史消息中取出本次输入，并在返回的历史列表中移除它。
     *
     * 取最后一条 user 消息作为当前输入；若因异常情况取不到，则回退使用接口传入的文本。
     */
    private String extractAndRemoveLastUserMessage(List<Message> history, String fallback) {
        for (int i = history.size() - 1; i >= 0; i--) {
            Message message = history.get(i);
            if (message instanceof UserMessage) {
                String text = message.getText() == null || message.getText().isBlank()
                        ? fallback
                        : message.getText();
                history.remove(i);
                return text;
            }
        }
        return fallback;
    }
}
