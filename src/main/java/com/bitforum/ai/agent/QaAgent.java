package com.bitforum.ai.agent;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 论坛问答助手 Agent（M13）。
 *
 * M13 只做「带多轮记忆的对话」，检索与工具调用分别在 M15、M14 接入。
 * 系统提示词已按论坛场景约束：站内优先、不编造、不越权。
 */
@Component
public class QaAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(QaAgent.class);

    private static final String SYSTEM_PROMPT = """
            你是 BitForum 技术社区的站内 AI 助手，面向中文开发者。

            回答要求：
            1. 用简体中文回答，条理清晰，必要时使用简短的列表。
            2. 优先基于站内内容与用户提供的上下文作答；不得编造站内不存在的文章、数据或统计。
            3. 不确定时要明确说明"站内暂时没有相关信息"，并给出可行的排查方向。
            4. 不讨论违法违规内容；不泄露他人隐私；不代替用户执行未确认的写操作。

            当前能力边界：你还没有接入站内检索与工具调用，如果用户询问站内具体文章，
            应说明该能力正在建设中，并建议其使用站内搜索。
            """;

    private final ObjectProvider<ChatClient> chatClientProvider;

    public QaAgent(ObjectProvider<ChatClient> chatClientProvider) {
        this.chatClientProvider = chatClientProvider;
    }

    @Override
    public AgentType type() {
        return AgentType.QA;
    }

    @Override
    public AgentResponse execute(AgentContext context, List<Message> history) {
        ChatClient chatClient = chatClientProvider.getIfAvailable();
        if (chatClient == null) {
            // AI 未启用（未配置 API Key）时不抛异常，返回降级文案，保证论坛主流程可用
            log.debug("ChatClient 不可用，QA 走降级回答");
            return AgentResponse.degraded("AI 助手当前未启用（未配置 DEEPSEEK_API_KEY），请联系管理员。");
        }

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SYSTEM_PROMPT));
        if (history != null) {
            messages.addAll(history);
        }
        messages.add(new UserMessage(context.userMessage()));

        try {
            ChatResponse response = chatClient.prompt()
                    .messages(messages)
                    .call()
                    .chatResponse();

            String content = response == null || response.getResult() == null
                    || response.getResult().getOutput() == null
                            ? ""
                            : response.getResult().getOutput().getText();

            if (content == null || content.isBlank()) {
                log.warn("模型返回空内容，conversationId={}", context.conversationId());
                return AgentResponse.degraded("AI 暂时没有生成有效回答，请稍后重试或换一种问法。");
            }

            return AgentResponse.of(content, promptTokens(response),
                    completionTokens(response), totalTokens(response));
        } catch (RuntimeException e) {
            // 大模型不可用（网络、额度、限流）不应影响论坛其他功能
            log.error("调用大模型失败，conversationId={}", context.conversationId(), e);
            return AgentResponse.degraded("AI 服务暂时不可用，请稍后重试。");
        }
    }

    private Integer promptTokens(ChatResponse response) {
        return response.getMetadata() == null || response.getMetadata().getUsage() == null
                ? null
                : response.getMetadata().getUsage().getPromptTokens();
    }

    private Integer completionTokens(ChatResponse response) {
        return response.getMetadata() == null || response.getMetadata().getUsage() == null
                ? null
                : response.getMetadata().getUsage().getCompletionTokens();
    }

    private Integer totalTokens(ChatResponse response) {
        return response.getMetadata() == null || response.getMetadata().getUsage() == null
                ? null
                : response.getMetadata().getUsage().getTotalTokens();
    }
}
