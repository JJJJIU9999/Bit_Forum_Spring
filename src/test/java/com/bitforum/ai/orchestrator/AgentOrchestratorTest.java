package com.bitforum.ai.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.bitforum.ai.agent.Agent;
import com.bitforum.ai.agent.AgentContext;
import com.bitforum.ai.agent.AgentResponse;
import com.bitforum.ai.agent.AgentType;
import com.bitforum.ai.agent.QaAgent;
import com.bitforum.ai.dto.AiConversationResponse;
import com.bitforum.ai.dto.AiMessageResponse;
import com.bitforum.ai.entity.AiConversation;
import com.bitforum.ai.mapper.AiConversationMapper;
import com.bitforum.ai.service.AiConversationService;

/**
 * AgentOrchestrator 集成测试（M13）。
 *
 * 测试策略：额外注册一个 RECOMMEND 类型的测试 Agent（见 TestAgentConfig），
 * 并把会话的 agentType 置为 RECOMMEND 来驱动路由。
 *
 * 这样做而不是直接 mock QaAgent，原因有二：
 * 1. 真实的 QaAgent 保留在容器中，其 type() 返回值会被真实执行，可验证路由注册是否正确；
 * 2. 避免 @MockitoBean 替换 @Component 时 type() 返回 null 导致编排器注册失败。
 *
 * 测试 Agent 不会发起真实大模型请求。
 */
@SpringBootTest
@Transactional
@Import(AgentOrchestratorTest.TestAgentConfig.class)
class AgentOrchestratorTest {

    /**
     * 测试专用 Agent。
     *
     * 刻意使用真实实现类而不是 Mockito mock：AgentOrchestrator 在构造阶段就会读取
     * 每个 Agent 的 type() 来建立路由表，而 mock 的打桩在该时点尚未生效（会返回 null，
     * 导致 Agent 被判定为无法路由）。真实实现的 type() 是硬编码的，与打桩时序无关。
     */
    @TestConfiguration
    static class TestAgentConfig {
        @Bean
        TestRoutingAgent testRoutingAgent() {
            return new TestRoutingAgent();
        }
    }

    /** 固定 RECOMMEND 类型、回答内容可切换的测试 Agent。 */
    static class TestRoutingAgent implements Agent {
        private volatile AgentResponse response =
                AgentResponse.of("这是模拟的 AI 回答", 100, 20, 120);
        private volatile List<Message> lastHistory;
        private volatile int callCount;

        @Override
        public AgentType type() {
            return AgentType.RECOMMEND;
        }

        @Override
        public AgentResponse execute(AgentContext context, List<Message> history) {
            this.lastHistory = history;
            this.callCount++;
            return response;
        }

        /** 重置所有可变状态。方法级隔离必须显式做：同一测试类的多个方法共用同一个 Spring 上下文与 bean 实例。 */
        void reset() {
            this.response = AgentResponse.of("这是模拟的 AI 回答", 100, 20, 120);
            this.lastHistory = null;
            this.callCount = 0;
        }

        void willReturn(AgentResponse response) {
            this.response = response;
        }

        List<Message> lastHistory() {
            return lastHistory;
        }

        int callCount() {
            return callCount;
        }
    }

    @Autowired
    private AgentOrchestrator orchestrator;
    @Autowired
    private AiConversationService conversationService;
    @Autowired
    private AiConversationMapper aiConversationMapper;
    @Autowired
    private TestRoutingAgent testRoutingAgent;

    /** 真实 QaAgent 保留在容器里用于验证路由注册；它不会被本测试调用。 */
    @MockitoBean
    private QaAgent qaAgent;

    @BeforeEach
    void resetTestAgent() {
        // 必须在每个测试方法前重置：Spring 上下文被整个测试类复用，
        // 若不重置，上一轮设置的降级回答与调用计数会污染后续测试。
        testRoutingAgent.reset();
    }

    @Test
    void shouldPersistBothUserAndAssistantMessages() {
        Long conversationId = createConversation(83001L, AgentType.RECOMMEND);

        AiMessageResponse answer = orchestrator.chat(conversationId, 83001L, "论坛怎么发帖？");

        assertEquals("assistant", answer.getRole());
        assertEquals("这是模拟的 AI 回答", answer.getContent());
        assertNotNull(answer.getId(), "助手回答应已落库并获得主键");

        List<AiMessageResponse> history = conversationService.listMessages(conversationId, 83001L);
        assertEquals(2, history.size(), "用户提问与助手回答都应落库");
        assertEquals("user", history.get(0).getRole());
        assertEquals("论坛怎么发帖？", history.get(0).getContent());
        assertEquals("assistant", history.get(1).getRole());
    }

    @Test
    void shouldPassConversationHistoryToAgent() {
        Long conversationId = createConversation(83002L, AgentType.RECOMMEND);

        orchestrator.chat(conversationId, 83002L, "第一个问题");

        // 第二次对话时，Agent 应收到包含第一轮问答的历史
        orchestrator.chat(conversationId, 83002L, "第二个问题");

        List<Message> history = testRoutingAgent.lastHistory();
        assertNotNull(history, "Agent 应被调用并记录历史");
        assertEquals(2, history.size(), "历史应包含上一轮的用户提问与助手回答");
        assertTrue(history.stream().anyMatch(m -> "第一个问题".equals(m.getText())),
                "历史中应包含上一轮提问");
    }

    @Test
    void shouldRouteToAgentMatchingConversationType() {
        Long conversationId = createConversation(83008L, AgentType.RECOMMEND);

        orchestrator.chat(conversationId, 83008L, "路由验证");

        // 验证确实按会话类型路由到了对应 Agent，而不是回退到 QA
        assertEquals(1, testRoutingAgent.callCount(), "应按 agentType 路由到 RECOMMEND 类型的 Agent");
    }

    @Test
    void shouldUpdateConversationStats() {
        Long conversationId = createConversation(83003L, AgentType.RECOMMEND);

        orchestrator.chat(conversationId, 83003L, "统计测试");

        AiConversation updated = aiConversationMapper.selectById(conversationId);
        assertEquals(2, updated.getMessageCount(), "一次对话应计入两条消息");
        assertEquals(120, updated.getTotalTokens(), "应累计 Token 消耗");
    }

    @Test
    void shouldRenameConversationFromFirstMessage() {
        Long conversationId = createConversation(83004L, AgentType.RECOMMEND);

        orchestrator.chat(conversationId, 83004L, "如何配置 Redis 缓存？");

        AiConversation updated = aiConversationMapper.selectById(conversationId);
        assertEquals("如何配置 Redis 缓存？", updated.getTitle(), "首轮对话应自动生成会话标题");
    }

    @Test
    void shouldRejectOtherUsersConversation() {
        Long conversationId = createConversation(83005L, AgentType.RECOMMEND);

        // 另一个用户尝试访问，应被拒绝且不产生任何消息
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orchestrator.chat(conversationId, 83999L, "越权提问"));
        assertEquals(403, ex.getStatusCode().value());

        assertTrue(conversationService.listMessages(conversationId, 83005L).isEmpty(),
                "越权请求不应写入任何消息");
    }

    @Test
    void shouldRejectUnknownConversation() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> orchestrator.chat(999999999L, 83006L, "不存在的会话"));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void shouldPersistDegradedAnswerWhenAiUnavailable() {
        Long conversationId = createConversation(83007L, AgentType.RECOMMEND);
        testRoutingAgent.willReturn(AgentResponse.degraded("AI 服务暂时不可用，请稍后重试。"));

        AiMessageResponse answer = orchestrator.chat(conversationId, 83007L, "降级测试");

        // 降级回答也必须正常落库，保证用户能在历史中看到这条记录
        assertEquals("AI 服务暂时不可用，请稍后重试。", answer.getContent());
        assertEquals(2, conversationService.listMessages(conversationId, 83007L).size());
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建会话并把 agentType 改成指定类型。
     * 走 Service 创建再更新，而不是直接 insert，目的是同时覆盖 Service 的默认值逻辑。
     */
    private Long createConversation(Long userId, AgentType agentType) {
        AiConversationResponse created = conversationService.createConversation(userId, null);
        AiConversation conversation = aiConversationMapper.selectById(created.getId());
        conversation.setAgentType(agentType.name());
        aiConversationMapper.updateById(conversation);
        return created.getId();
    }
}
