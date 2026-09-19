package com.bitforum.ai.usage;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.bitforum.ai.dto.AiUsageDtos;
import com.bitforum.ai.dto.AiUsageOverviewResponse;
import com.bitforum.ai.mapper.AiUsageStatMapper;

/**
 * 用量查询服务（M18 管理端）。
 *
 * <p>聚合全部下推到 SQL（见 {@link AiUsageStatMapper}）：明细行数会随调用量增长，
 * 把明细捞进内存再聚合是不可持续的。
 */
@Service
public class AiUsageQueryService {

    /** 概览里展示的"消耗最高的用户"条数。 */
    private static final int TOP_USER_LIMIT = 10;
    /** 查询区间下限与上限：上限防止把整表扫出来。 */
    private static final int MIN_DAYS = 1;
    private static final int MAX_DAYS = 90;

    private static final String PRICE_NOTE =
            "成本为按配置单价（输入/输出分列）估算的参考值，非账单金额；单价可能随供应商调价而变化。";

    private final AiUsageStatMapper usageMapper;
    private final AiUsageRecorder usageRecorder;

    public AiUsageQueryService(AiUsageStatMapper usageMapper, AiUsageRecorder usageRecorder) {
        this.usageMapper = usageMapper;
        this.usageRecorder = usageRecorder;
    }

    /** 最近 days 天（含今天）的用量概览。 */
    public AiUsageOverviewResponse overview(int days) {
        int normalized = Math.min(MAX_DAYS, Math.max(MIN_DAYS, days));
        LocalDate from = LocalDate.now().minusDays(normalized - 1L);

        AiUsageOverviewResponse response = new AiUsageOverviewResponse();
        response.setDays(normalized);
        response.setTotals(usageMapper.selectTotals(from));
        response.setDaily(usageMapper.selectDaily(from));
        response.setByAgent(usageMapper.selectByAgent(from));
        response.setTopUsers(usageMapper.selectTopUsers(from, TOP_USER_LIMIT));
        response.setInputPricePerMillion(usageRecorder.inputPricePerMillion());
        response.setOutputPricePerMillion(usageRecorder.outputPricePerMillion());
        response.setPriceNote(PRICE_NOTE);
        return response;
    }

    /** 按天明细（供"用量趋势"单独使用）。 */
    public java.util.List<AiUsageDtos.DailyItem> daily(int days) {
        int normalized = Math.min(MAX_DAYS, Math.max(MIN_DAYS, days));
        return usageMapper.selectDaily(LocalDate.now().minusDays(normalized - 1L));
    }
}
