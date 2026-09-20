package com.bitforum.ai.recommend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.bitforum.ai.entity.AiRecommendLog;
import com.bitforum.ai.mapper.AiRecommendLogMapper;
import com.bitforum.ai.recommend.RecommendFusionService.RecommendCandidate;
import com.bitforum.ai.recommend.RecommendRecallService.RecallRequest;
import com.bitforum.ai.recommend.RecommendRecallService.RecallSource;
import com.bitforum.ai.recommend.RecommendRecallService.RecalledArticle;
import com.bitforum.ai.recommend.RecommendService.RecommendRequest;
import com.bitforum.ai.recommend.RecommendService.RecommendResult;
import com.bitforum.ai.recommend.RecommendService.RecommendedArticle;
import com.bitforum.ai.mapper.AiUsageStatMapper;
import com.bitforum.ai.trace.TraceDegradeReason;
import com.bitforum.ai.trace.TraceRecorder;
import com.bitforum.ai.usage.AiTokenBudgetGuard;
import com.bitforum.entity.Article;
import com.bitforum.entity.ArticleFavorite;
import com.bitforum.entity.Category;
import com.bitforum.mapper.ArticleFavoriteMapper;
import com.bitforum.mapper.ArticleMapper;
import com.bitforum.mapper.CategoryMapper;

/**
 * 推荐编排的单元测试（M17）。
 *
 * <p>重点覆盖三件事：
 * <ol>
 *   <li><b>降级路径必须能独立工作</b>：没有模型理由时，推荐列表照样要能返回
 *       （handoff 硬约束第 9 条）—— 这条路径是 M17 当前唯一可用的形态，不能出错；</li>
 *   <li><b>候选为空是正常结果而不是异常</b>：文章库小或全被排除时不应抛错；</li>
 *   <li><b>记录落库失败不能让推荐失败</b>：落库是评测的副产品，不是主流程。</li>
 * </ol>
 */
class RecommendServiceTest {

    private RecommendRecallService recallService;
    private RecommendFusionService fusionService;
    private RecommendReasonAgent reasonAgent;
    private ArticleMapper articleMapper;
    private ArticleFavoriteMapper favoriteMapper;
    private CategoryMapper categoryMapper;
    private AiRecommendLogMapper logMapper;
    private RecommendService service;

    /**
     * 纯单测里使用 {@code LambdaQueryWrapper} 需要 MyBatis-Plus 的实体元信息缓存，
     * 否则会抛 "can not find lambda cache for this entity"。
     * 这里手动初始化推荐链路涉及的三个实体；改成 {@code @SpringBootTest} 也能解决，
     * 但那会让这个纯逻辑测试反过来依赖数据库，得不偿失。
     */
    @BeforeAll
    static void initMybatisPlusMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Article.class);
        TableInfoHelper.initTableInfo(assistant, ArticleFavorite.class);
        TableInfoHelper.initTableInfo(assistant, Category.class);
    }

    @BeforeEach
    void setUp() {
        recallService = mock(RecommendRecallService.class);
        fusionService = mock(RecommendFusionService.class);
        reasonAgent = mock(RecommendReasonAgent.class);
        // 默认按"理由生成降级"处理：这样既有用例测的仍是确定性排序链路
        when(reasonAgent.generate(any(), any(), anyBoolean())).thenReturn(
                RecommendReasonAgent.ReasonOutcome.degraded(TraceDegradeReason.AI_DISABLED,
                        "deepseek-flash", 0L, "测试中不生成理由"));
        articleMapper = mock(ArticleMapper.class);
        favoriteMapper = mock(ArticleFavoriteMapper.class);
        categoryMapper = mock(CategoryMapper.class);
        logMapper = mock(AiRecommendLogMapper.class);
        // M18 收尾：预算闸门用真实实现 + mock 的用量查询（无当日记录 → 放行），
        // 既有用例不需要为预算写额外打桩
        service = new RecommendService(recallService, fusionService, reasonAgent, articleMapper,
                favoriteMapper, categoryMapper, logMapper, "deepseek-flash", 20, true, true, true,
                mock(TraceRecorder.class), allowAllBudget());
    }

    /** 候选为空：返回空列表 + 降级标记，不抛异常、不写记录。 */
    @Test
    void shouldReturnEmptyResultWhenNothingRecalled() {
        when(recallService.recall(any())).thenReturn(List.of());
        when(fusionService.fuse(any(), any(), anyInt())).thenReturn(List.of());

        RecommendResult result = service.recommend(RecommendRequest.forArticleDetail(13L, 87L, 5));

        assertTrue(result.isEmpty());
        assertTrue(result.degraded(), "没有模型参与时必须是降级形态");
        assertEquals(0, result.recallCount());
        verify(logMapper, never()).insert(any(AiRecommendLog.class));
    }

    /** 正常路径：带上标题与板块名，排名从 1 连续编号，并逐条写入推荐记录。 */
    @Test
    void shouldReturnRankedArticlesAndPersistLog() {
        when(recallService.recall(any())).thenReturn(List.of(
                new RecalledArticle(85L, RecallSource.VECTOR, 1, 0.77),
                new RecalledArticle(87L, RecallSource.HOT, 1, 3.0)));
        when(fusionService.fuse(any(), any(), anyInt())).thenReturn(List.of(
                new RecommendCandidate(85L, 0.0325, List.of("vector"), "{\"vector\":1}", 1),
                new RecommendCandidate(87L, 0.0164, List.of("hot"), "{\"hot\":1}", 2)));
        when(articleMapper.selectList(any())).thenReturn(List.of(
                article(85L, "Spring Boot 论坛项目实践", 16L),
                article(87L, "Redis 热点数据同步方案", 16L)));
        when(categoryMapper.selectList(any())).thenReturn(List.of(category(16L, "技术")));

        RecommendResult result = service.recommend(RecommendRequest.forArticleDetail(13L, 86L, 5));

        assertEquals(2, result.articles().size());
        assertEquals(1, result.articles().get(0).rank());
        assertEquals("Spring Boot 论坛项目实践", result.articles().get(0).title());
        assertEquals("技术", result.articles().get(0).categoryName());
        assertNull(result.articles().get(0).reason(), "当前不生成理由，前端要能处理 null");
        assertEquals(2, result.recallCount());

        ArgumentCaptor<AiRecommendLog> captor = ArgumentCaptor.forClass(AiRecommendLog.class);
        verify(logMapper, times(2)).insert(captor.capture());
        List<AiRecommendLog> saved = captor.getAllValues();
        assertEquals(List.of(1, 2), saved.stream().map(AiRecommendLog::getRankNo).toList());
        assertEquals(AiRecommendLog.SCENE_ARTICLE_DETAIL, saved.get(0).getScene());
        assertEquals(13L, saved.get(0).getUserId());
        assertEquals(86L, saved.get(0).getSourceArticleId());
        assertEquals("vector", saved.get(0).getRecallSources());
        assertTrue(saved.get(0).getDegraded(), "降级形态要如实记录，评测时才能区分");
        assertNull(saved.get(0).getExperimentTag(), "线上请求不带实验批次");
    }

    /** 补全阶段查不到的文章要跳过，且排名重新连续编号。 */
    @Test
    void shouldSkipUnavailableArticlesAndRenumberRanks() {
        when(recallService.recall(any())).thenReturn(List.of(
                new RecalledArticle(85L, RecallSource.VECTOR, 1, 0.77),
                new RecalledArticle(87L, RecallSource.VECTOR, 2, 0.76)));
        when(fusionService.fuse(any(), any(), anyInt())).thenReturn(List.of(
                new RecommendCandidate(85L, 0.0164, List.of("vector"), "{\"vector\":1}", 1),
                new RecommendCandidate(87L, 0.0161, List.of("vector"), "{\"vector\":2}", 2)));
        // 87 在这两步之间被下架，查不到了
        when(articleMapper.selectList(any())).thenReturn(List.of(
                article(85L, "Spring Boot 论坛项目实践", 16L)));

        RecommendResult result = service.recommend(RecommendRequest.forArticleDetail(13L, null, 5));

        assertEquals(1, result.articles().size());
        assertEquals(85L, result.articles().get(0).articleId());
        assertEquals(1, result.articles().get(0).rank());
    }

    /** 未登录访客没有个人数据：不该去查收藏表（也查不到东西）。 */
    @Test
    void shouldNotQueryPersonalDataForAnonymousVisitor() {
        when(recallService.recall(any())).thenReturn(List.of());
        when(fusionService.fuse(any(), any(), anyInt())).thenReturn(List.of());

        service.recommend(new RecommendRequest(AiRecommendLog.SCENE_ARTICLE_DETAIL, null, 87L, 5, null));

        verify(favoriteMapper, never()).selectList(any());
    }

    /** 落库失败只记日志：推荐结果必须照常返回。 */
    @Test
    void shouldStillReturnRecommendationsWhenLogPersistenceFails() {
        when(recallService.recall(any())).thenReturn(List.of(
                new RecalledArticle(85L, RecallSource.VECTOR, 1, 0.77)));
        when(fusionService.fuse(any(), any(), anyInt())).thenReturn(List.of(
                new RecommendCandidate(85L, 0.0164, List.of("vector"), "{\"vector\":1}", 1)));
        when(articleMapper.selectList(any())).thenReturn(List.of(
                article(85L, "Spring Boot 论坛项目实践", 16L)));
        when(logMapper.insert(any(AiRecommendLog.class))).thenThrow(new IllegalStateException("数据库不可用"));

        RecommendResult result = service.recommend(RecommendRequest.forArticleDetail(13L, 87L, 5));

        assertEquals(1, result.articles().size());
        assertEquals("Spring Boot 论坛项目实践", result.articles().get(0).title());
    }

    /** 纯热榜基线：只走热榜一路、按热度分值排序，并带上实验批次供评测分组统计。 */
    @Test
    void shouldBuildHotBaselineFromHotChannelOnly() {
        when(recallService.recall(any())).thenReturn(List.of(
                // 召回层给出的顺序刻意与热度分值不一致，用来验证基线是按分值重排的
                new RecalledArticle(85L, RecallSource.HOT, 1, 1.0),
                new RecalledArticle(87L, RecallSource.HOT, 2, 3.0)));
        when(articleMapper.selectList(any())).thenReturn(List.of(
                article(85L, "Spring Boot 论坛项目实践", 16L),
                article(87L, "Redis 热点数据同步方案", 16L)));
        when(categoryMapper.selectList(any())).thenReturn(List.of(category(16L, "技术")));

        RecommendResult result = service.hotBaseline(
                RecommendRequest.forArticleDetail(13L, null, 5), AiRecommendLog.TAG_BASELINE_HOT);

        assertEquals(List.of(87L, 85L), result.articles().stream()
                        .map(RecommendedArticle::articleId).toList(),
                "基线必须按热度分值降序，而不是按召回顺序");

        // 基线只应当走热榜这一路：混入另外两路就不再是"纯热榜"了
        ArgumentCaptor<RecallRequest> requestCaptor = ArgumentCaptor.forClass(RecallRequest.class);
        verify(recallService).recall(requestCaptor.capture());
        assertTrue(requestCaptor.getValue().includes(RecallSource.HOT));
        assertFalse(requestCaptor.getValue().includes(RecallSource.VECTOR));
        assertFalse(requestCaptor.getValue().includes(RecallSource.FOLLOW));

        // 落库带上实验批次，评测才能按批次分组统计命中率
        ArgumentCaptor<AiRecommendLog> logCaptor = ArgumentCaptor.forClass(AiRecommendLog.class);
        verify(logMapper, times(2)).insert(logCaptor.capture());
        assertEquals(AiRecommendLog.TAG_BASELINE_HOT, logCaptor.getAllValues().get(0).getExperimentTag());
        assertEquals("hot", logCaptor.getAllValues().get(0).getRecallSources());
    }

    /** 理由生成成功时，理由应回填到对应文章上，且整体不再标记为降级。 */
    @Test
    void shouldAttachReasonsWhenGenerationSucceeds() {
        when(recallService.recall(any())).thenReturn(List.of(
                new RecalledArticle(85L, RecallSource.VECTOR, 1, 0.77)));
        when(fusionService.fuse(any(), any(), anyInt())).thenReturn(List.of(
                new RecommendCandidate(85L, 0.0164, List.of("vector"), "{\"vector\":1}", 1)));
        when(articleMapper.selectList(any())).thenReturn(List.of(
                article(85L, "Spring Boot 论坛项目实践", 16L)));
        when(categoryMapper.selectList(any())).thenReturn(List.of(category(16L, "技术")));
        when(reasonAgent.generate(any(), any(), anyBoolean())).thenReturn(
                RecommendReasonAgent.ReasonOutcome.of(
                        Map.of(85L, "与你正在看的这篇主题相近"), "deepseek-flash", 120, 30, 150, 800L));

        RecommendResult result = service.recommend(RecommendRequest.forArticleDetail(13L, 87L, 5));

        assertEquals(1, result.articles().size());
        assertEquals("与你正在看的这篇主题相近", result.articles().get(0).reason());
        assertFalse(result.degraded(), "理由生成成功时不应标记为降级");

        ArgumentCaptor<AiRecommendLog> captor = ArgumentCaptor.forClass(AiRecommendLog.class);
        verify(logMapper).insert(captor.capture());
        assertEquals("与你正在看的这篇主题相近", captor.getValue().getReason());
        assertFalse(captor.getValue().getDegraded(), "理由落库后 degraded 应为 false");
    }

    /** M18 收尾：预算闸门 —— 真实实现 + mock 用量查询（查不到当日用量即放行）。 */
    private AiTokenBudgetGuard allowAllBudget() {
        return new AiTokenBudgetGuard(mock(AiUsageStatMapper.class), true,
                200000L, new java.math.BigDecimal("2.0"), 4000, 1024);
    }

    /** M18 收尾：当日用量超预算时跳过理由生成，但**列表与排序完全不受影响**。 */
    @Test
    void shouldSkipReasonGenerationWhenDailyBudgetExceeded() {
        AiUsageStatMapper mapper = mock(AiUsageStatMapper.class);
        com.bitforum.ai.dto.AiUsageDtos.UserUsage usage = new com.bitforum.ai.dto.AiUsageDtos.UserUsage();
        usage.setTotalTokens(999_999L);
        usage.setCost(new java.math.BigDecimal("0.01"));
        when(mapper.selectUserDaily(any(), any())).thenReturn(usage);
        AiTokenBudgetGuard exhausted = new AiTokenBudgetGuard(mapper, true, 1000L,
                new java.math.BigDecimal("2.0"), 4000, 1024);

        RecommendService budgeted = new RecommendService(recallService, fusionService, reasonAgent,
                articleMapper, favoriteMapper, categoryMapper, logMapper, "deepseek-flash", 20, true, true,
                true, mock(TraceRecorder.class), exhausted);
        when(recallService.recall(any())).thenReturn(List.of(
                new RecalledArticle(85L, RecallSource.VECTOR, 1, 0.77)));
        when(fusionService.fuse(any(), any(), anyInt())).thenReturn(List.of(
                new RecommendCandidate(85L, 0.0164, List.of("vector"), "{\"vector\":1}", 1)));
        when(articleMapper.selectList(any())).thenReturn(List.of(
                article(85L, "Spring Boot 论坛项目实践", 16L)));
        when(categoryMapper.selectList(any())).thenReturn(List.of(category(16L, "技术")));

        RecommendResult result = budgeted.recommend(RecommendRequest.forArticleDetail(13L, 87L, 5));

        assertEquals(1, result.articles().size(), "超预算只影响理由，不影响推荐列表");
        assertTrue(result.degraded(), "无理由时应标记为降级");
        assertNull(result.articles().get(0).reason());
        verify(reasonAgent, never()).generate(any(), any(), anyBoolean());
    }

    /** 关闭理由生成时不应调用模型（离线评测依赖这个开关，避免白烧额度）。 */
    @Test
    void shouldSkipReasonGenerationWhenDisabled() {
        RecommendService withoutReason = new RecommendService(recallService, fusionService, reasonAgent,
                articleMapper, favoriteMapper, categoryMapper, logMapper, "deepseek-flash", 20, true, true,
                false, mock(TraceRecorder.class), allowAllBudget());
        when(recallService.recall(any())).thenReturn(List.of(
                new RecalledArticle(85L, RecallSource.VECTOR, 1, 0.77)));
        when(fusionService.fuse(any(), any(), anyInt())).thenReturn(List.of(
                new RecommendCandidate(85L, 0.0164, List.of("vector"), "{\"vector\":1}", 1)));
        when(articleMapper.selectList(any())).thenReturn(List.of(
                article(85L, "Spring Boot 论坛项目实践", 16L)));

        RecommendResult result = withoutReason.recommend(RecommendRequest.forArticleDetail(13L, 87L, 5));

        assertTrue(result.degraded(), "关闭理由生成时应保持降级形态（列表仍可用）");
        assertNull(result.articles().get(0).reason());
        verify(reasonAgent, never()).generate(any(), any(), anyBoolean());
    }

    /**
     * "根据提问推荐"场景：只保留向量通道命中的候选。
     *
     * 热度与关注可以给相关候选加分排序，但不能把与提问无关的文章带进来 ——
     * 否则会出现"问 Redis 却推一屏 MySQL"的答非所问。
     */
    @Test
    void shouldKeepOnlyQueryRelevantCandidatesWhenQueryDriven() {
        when(recallService.recall(any())).thenReturn(List.of(
                // 与提问相关（向量命中）
                new RecalledArticle(900L, RecallSource.VECTOR, 1, 0.80),
                // 只是热/关注命中，与提问无关
                new RecalledArticle(901L, RecallSource.HOT, 1, 30.0),
                new RecalledArticle(902L, RecallSource.FOLLOW, 1, -1.0)));
        when(fusionService.fuse(any(), any(), anyInt())).thenReturn(List.of(
                new RecommendCandidate(900L, 0.0164, List.of("vector"), "{\"vector\":1}", 1)));
        when(articleMapper.selectList(any())).thenReturn(List.of(
                article(900L, "Redis 缓存穿透与雪崩的实战解法", 16L)));

        RecommendResult result = service.recommend(
                RecommendRequest.forQuery("Redis 缓存穿透怎么处理", 13L, 5));

        ArgumentCaptor<List<RecalledArticle>> captor = ArgumentCaptor.forClass(List.class);
        verify(fusionService).fuse(captor.capture(), any(), anyInt());
        assertEquals(List.of(900L), captor.getValue().stream()
                        .map(RecalledArticle::articleId).distinct().toList(),
                "与提问无关的候选不应进入融合");
        assertEquals(1, result.articles().size());
    }

    private Article article(Long id, String title, Long categoryId) {
        Article article = new Article();
        article.setId(id);
        article.setTitle(title);
        article.setCategoryId(categoryId);
        return article;
    }

    private Category category(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        return category;
    }
}
