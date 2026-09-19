package com.bitforum.ai.analyst;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;

import com.bitforum.dto.AdminDashboardSummaryResponse;
import com.bitforum.dto.DashboardArticleStats;
import com.bitforum.dto.DashboardHotArticle;
import com.bitforum.dto.DashboardUserStats;
import com.bitforum.service.AdminDashboardService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 运营分析 Agent 的单元测试（M17）。
 *
 * <p>只测**不依赖模型**的部分：降级链路、快照留存、异常不外抛。
 * 真实的模型调用（工具调用 + 数字保真）由 T10 探针与将来的冒烟测试覆盖，
 * 因为那部分需要真实 API Key，不应放进常规测试。
 */
class AnalystAgentTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 模型不可用时：必须降级，但**统计快照仍然要有**（统计是本地算的，与 AI 无关）。 */
    @Test
    void shouldKeepSnapshotWhenModelUnavailable() {
        AdminDashboardService dashboardService = mock(AdminDashboardService.class);
        when(dashboardService.summary()).thenReturn(sampleSummary());

        AnalystAgent agent = new AnalystAgent(unavailableChatClient(), dashboardService,
                objectMapper, "deepseek-flash");

        AnalystAgent.InsightOutcome outcome = agent.analyze();

        assertTrue(outcome.degraded(), "模型不可用时应走降级链路");
        assertNull(outcome.content(), "降级时不应有洞察正文");
        assertNotNull(outcome.dataSnapshot(), "降级也必须保留统计快照：报告失败但依据要留痕");
        assertTrue(outcome.dataSnapshot().contains("\"total\":5"), "快照应包含真实统计：" + outcome.dataSnapshot());
        assertEquals(0, outcome.toolCallCount(), "降级时没有发生工具调用");
        assertNotNull(outcome.errorMessage());
    }

    /** 统计聚合本身失败：不抛异常，返回降级结果（论坛主流程不能被 AI 拖垮）。 */
    @Test
    void shouldDegradeInsteadOfThrowingWhenDashboardFails() {
        AdminDashboardService dashboardService = mock(AdminDashboardService.class);
        when(dashboardService.summary()).thenThrow(new IllegalStateException("数据库连接中断"));

        AnalystAgent agent = new AnalystAgent(unavailableChatClient(), dashboardService,
                objectMapper, "deepseek-flash");

        AnalystAgent.InsightOutcome outcome = agent.analyze();

        assertTrue(outcome.degraded());
        assertNull(outcome.dataSnapshot(), "统计都没取到，快照应为空而不是伪造一份");
        assertNotNull(outcome.errorMessage());
        assertTrue(outcome.errorMessage().contains("数据库连接中断"), "失败原因应保留：" + outcome.errorMessage());
    }

    /** 工具类：返回同一份快照，并统计调用次数（用于发现"模型没看数据就写结论"）。 */
    @Test
    void shouldExposeSameSnapshotAndCountToolCalls() {
        AdminDashboardSummaryResponse summary = sampleSummary();
        AnalystTools tools = new AnalystTools(summary);

        assertEquals(0, tools.callCount());
        assertEquals(summary, tools.getDashboardSummary());
        assertEquals(summary, tools.getDashboardSummary());
        assertEquals(2, tools.callCount());
    }

    /** 快照里含热门文章，便于核对"报告提到的热榜文章"是否真实存在。 */
    @Test
    void shouldSerializeHotArticlesIntoSnapshot() throws Exception {
        AdminDashboardService dashboardService = mock(AdminDashboardService.class);
        when(dashboardService.summary()).thenReturn(sampleSummary());

        AnalystAgent agent = new AnalystAgent(unavailableChatClient(), dashboardService,
                objectMapper, "deepseek-flash");

        String snapshot = agent.analyze().dataSnapshot();

        assertNotNull(snapshot);
        // 快照是落库后用来对账的，必须是**可解析的结构化 JSON**，而不是一段随手拼的字符串
        JsonNode root = objectMapper.readTree(snapshot);
        assertEquals(5, root.get("userStats").get("total").asInt());
        assertEquals(87L, root.get("hotArticles").get(0).get("articleId").asLong());
        assertEquals("Redis 热点数据同步方案", root.get("hotArticles").get(0).get("title").asText());
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<ChatClient> unavailableChatClient() {
        ObjectProvider<ChatClient> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return provider;
    }

    private AdminDashboardSummaryResponse sampleSummary() {
        AdminDashboardSummaryResponse summary = new AdminDashboardSummaryResponse();

        DashboardUserStats userStats = new DashboardUserStats();
        userStats.setTotal(5L);
        userStats.setNormalUsers(4L);
        userStats.setAdmins(1L);
        userStats.setEnabled(4L);
        userStats.setDisabled(1L);
        summary.setUserStats(userStats);

        DashboardArticleStats articleStats = new DashboardArticleStats();
        articleStats.setTotal(10L);
        articleStats.setPublished(5L);
        articleStats.setPending(2L);
        summary.setArticleStats(articleStats);

        DashboardHotArticle hot = new DashboardHotArticle();
        hot.setArticleId(87L);
        hot.setTitle("Redis 热点数据同步方案");
        hot.setHotScore(3L);
        hot.setViewCount(0);
        hot.setLikeCount(1);
        summary.setHotArticles(List.of(hot));

        return summary;
    }
}
