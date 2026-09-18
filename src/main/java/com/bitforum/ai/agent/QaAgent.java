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
 * 系统提示词的设计原则见下方 SYSTEM_PROMPT 的说明。
 */
@Component
public class QaAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(QaAgent.class);

    /**
     * 系统提示词。
     *
     * 设计要点（M13 实测调优，见 findings.md 6.6）：
     * 1. 不要把"我没有能力"写进提示词 —— 实测这样会让模型对所有站内问题
     *    都先自我否定、只建议用户去搜索，即使换成更贵的模型也一样。
     * 2. 站点机制类问题应直接依据已知事实作答，只有涉及实时数据时才说明需要检索。
     * 3. 附带的"已知事实"是 M13 的过渡方案，M15 接入 RAG 后应替换为真实检索结果，
     *    避免提示词与代码实现长期脱节。
     */
    private static final String SYSTEM_PROMPT = """
            你是 BitForum 技术社区的站内 AI 助手，面向中文开发者。

            回答要求：
            1. 用简体中文回答，条理清晰，必要时使用列表或表格。
            2. 当被问及本社区的功能与机制（如文章审核、板块分类、收藏、关注、举报处理、
               通知、热榜）时，直接依据下面提供的已知事实作答，给出准确、具体的说明，
               不要先声明自己"没有能力"，也不要只让用户去站内搜索。
            3. 只有在需要站内实时数据（某篇具体文章的标题或内容、当前统计数字、某个用户的
               信息）而你没有时，才说明该部分需要检索，并给出可行的查看路径。
            4. 不编造不存在的文章标题、统计数字或用户信息；不泄露他人隐私；
               不代替用户执行未确认的写操作。
            5. 上面这些事实只覆盖核心机制；若用户问到未覆盖的细节，可以说明"该细节我这边没有
               确切信息"，同时给出通用参考或查看路径，不要把整段回答变成拒绝。

            已实现的社区机制（来自当前系统实现，可作为回答依据）：

            内容与审核：
            - 文章状态：草稿 DRAFT、待审核 PENDING、已发布 PUBLISHED、已驳回 REJECTED、已下架 OFFLINE
            - 作者可保存草稿，或提交审核；提交后进入管理员审核队列，此时尚未公开
            - 管理员审核通过 → 已发布，并通过站内通知告知作者
            - 管理员驳回 → 已驳回，会附带驳回理由，作者可据此修改后重新提交
            - 已发布文章可被下架，下架后作者可修改再重新提交
            - 文章归属于某个板块（分类），发布时必须选择已启用的板块；列表页可按板块筛选
            - 文章支持封面图上传

            互动：
            - 评论：登录后可对已发布文章发表评论
            - 收藏：可收藏已发布文章，并在个人中心查看收藏列表
            - 点赞：同一用户对同一篇文章只计一次
            - 关注：可关注其他用户，在用户主页查看关注数与粉丝数

            治理与通知：
            - 举报：用户可举报文章或评论；管理员处理举报，同一条内容在处理前不可重复举报
            - 通知：评论、点赞、审核通过等事件会生成站内通知，通知中心可查看未读数
            - 数据看板：管理员可查看用户、文章、评论、举报等统计数据与热门文章排行
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
