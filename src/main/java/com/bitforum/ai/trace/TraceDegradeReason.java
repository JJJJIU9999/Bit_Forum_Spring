package com.bitforum.ai.trace;

/**
 * 降级原因码（M18）。
 *
 * <p>M18 验收第 3 条：AI 不可用时全站要有**统一**的降级表现，且用户能看懂"为什么降级"。
 * 要做到"统一"，前提是原因**可枚举**——每个 Agent 各写一句自由文案是无法统计、也无法对齐的。
 * 因此这里定义一组有限的原因码，配合 {@code AiDegradeGuard}（M18 第 3 个模块）统一产出。
 *
 * <p>原因码与 {@code ai_execution_trace.degrade_reason} 一一对应，
 * 前端按它选择提示文案，管理端按它做统计。
 */
public final class TraceDegradeReason {

    /** AI 功能整体未启用（未配置 API Key / 开关关闭） */
    public static final String AI_DISABLED = "AI_DISABLED";
    /** 调用超时 */
    public static final String LLM_TIMEOUT = "LLM_TIMEOUT";
    /** 调用报错（网络、限流、额度） */
    public static final String LLM_ERROR = "LLM_ERROR";
    /** 模型返回空内容或无法解析 */
    public static final String EMPTY_RESPONSE = "EMPTY_RESPONSE";
    /** 检索失败，退化为无引用回答 */
    public static final String RETRIEVE_FAILED = "RETRIEVE_FAILED";
    /** 工具执行失败 */
    public static final String TOOL_FAILED = "TOOL_FAILED";
    /** 没有可用的输入（例如推荐列表为空，不需要生成理由） */
    public static final String NOTHING_TO_DO = "NOTHING_TO_DO";
    /** 超出 token 预算（TokenBudgetGuard，M18 时间充足才做） */
    public static final String BUDGET_EXCEEDED = "BUDGET_EXCEEDED";

    private TraceDegradeReason() {
    }

    /** 原因码 → 面向用户的一句话说明。未知原因码回退为通用文案。 */
    public static String userMessage(String reason) {
        if (reason == null) {
            return "AI 服务暂时不可用，已使用降级结果。";
        }
        return switch (reason) {
            case AI_DISABLED -> "AI 功能当前未启用，请联系管理员配置。";
            case LLM_TIMEOUT -> "AI 响应超时，已使用降级结果，请稍后重试。";
            case LLM_ERROR -> "AI 服务暂时不可用，已使用降级结果，请稍后重试。";
            case EMPTY_RESPONSE -> "AI 没有返回有效内容，请换一种问法或稍后重试。";
            case RETRIEVE_FAILED -> "站内检索暂时不可用，本次回答未引用站内文章。";
            case TOOL_FAILED -> "站内数据查询失败，本次回答可能不完整。";
            case NOTHING_TO_DO -> "当前没有需要 AI 处理的内容。";
            case BUDGET_EXCEEDED -> "已达到今日 AI 使用额度，请稍后再试。";
            default -> "AI 服务暂时不可用，已使用降级结果。";
        };
    }
}
