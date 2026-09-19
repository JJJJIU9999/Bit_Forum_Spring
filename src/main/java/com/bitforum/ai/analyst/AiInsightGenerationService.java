package com.bitforum.ai.analyst;

import java.util.Optional;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.bitforum.ai.entity.AiInsightReport;

/**
 * 运营洞察的触发与异步编排（M17）。
 *
 * <p>落地产品决策第 4 条：**管理员主动触发 + 异步生成 + 结果落库**。
 * 生成要调用大模型（实测数秒），因此绝不让 HTTP 请求同步等待 ——
 * 触发接口立刻返回一条 PENDING 记录，前端凭它轮询状态。
 *
 * <p><b>时序</b>：
 * <ol>
 *   <li>校验当前没有生成中的报告（防连点重复烧额度）；</li>
 *   <li>**先**写入 PENDING 记录，再提交异步任务 —— 顺序不能反，
 *       否则前端在任务提交后、记录落库前查询会什么都看不到；</li>
 *   <li>异步任务调用 {@link AnalystAgent} 生成，把结果写回同一条记录。</li>
 * </ol>
 *
 * <p><b>执行器是注入的</b>（{@code Executor}）：生产用单线程池，
 * 测试可以注入同步执行器（{@code Runnable::run}）来避免等待与不确定性。
 */
@Service
public class AiInsightGenerationService {

    private static final Logger log = LoggerFactory.getLogger(AiInsightGenerationService.class);

    private final AiInsightService insightService;
    private final AnalystAgent analystAgent;
    private final Executor insightExecutor;

    public AiInsightGenerationService(AiInsightService insightService,
                                      AnalystAgent analystAgent,
                                      @Qualifier("insightExecutor") Executor insightExecutor) {
        this.insightService = insightService;
        this.analystAgent = analystAgent;
        this.insightExecutor = insightExecutor;
    }

    /**
     * 受理一次生成请求并立即返回。
     *
     * @return 新写入的 PENDING 报告；若已有生成中的报告则返回空（调用方提示"正在生成"）
     */
    public Optional<AiInsightReport> trigger(Long requestedBy) {
        Optional<AiInsightReport> running = insightService.pending();
        if (running.isPresent()) {
            log.info("已有洞察正在生成（id={}），本次触发被忽略", running.get().getId());
            return Optional.empty();
        }

        AiInsightReport report = insightService.createPending(
                AiInsightReport.TRIGGER_MANUAL, requestedBy);

        insightExecutor.execute(() -> generate(report.getId()));
        return Optional.of(report);
    }

    /** 异步任务体：生成并写回结果。任何异常都要让记录离开 PENDING 状态。 */
    private void generate(Long reportId) {
        long startedAt = System.currentTimeMillis();
        try {
            AnalystAgent.InsightOutcome outcome = analystAgent.analyze();
            insightService.complete(reportId, outcome);
        } catch (RuntimeException exception) {
            // AnalystAgent 本身不抛异常（失败返回降级结果），这里是防御性兜底：
            // 记录绝不能永远停在 PENDING，否则前端会一直转圈、也再无法触发新的生成
            log.error("运营洞察异步生成异常：reportId={}", reportId, exception);
            insightService.markFailed(reportId, exception.getMessage());
        }
        log.debug("运营洞察异步任务结束：reportId={}，耗时 {} ms",
                reportId, System.currentTimeMillis() - startedAt);
    }
}
