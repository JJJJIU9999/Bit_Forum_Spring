package com.bitforum.ai.agent;

/**
 * Agent 执行结果（M13）。
 *
 * @param content          生成给用户的回答正文
 * @param promptTokens     输入 Token（模型未返回时为 null）
 * @param completionTokens 输出 Token（模型未返回时为 null）
 * @param totalTokens      合计 Token（模型未返回时为 null）
 * @param degraded         是否走了降级链路（AI 不可用时的兜底回答）
 */
public record AgentResponse(String content,
                            Integer promptTokens,
                            Integer completionTokens,
                            Integer totalTokens,
                            boolean degraded) {

    /** 正常回答。 */
    public static AgentResponse of(String content, Integer promptTokens,
                                   Integer completionTokens, Integer totalTokens) {
        return new AgentResponse(content, promptTokens, completionTokens, totalTokens, false);
    }

    /**
     * 降级回答：AI 不可用或调用失败时的兜底结果。
     * 论坛主流程不应因为 AI 故障而阻塞，因此这里始终返回可用文案。
     */
    public static AgentResponse degraded(String content) {
        return new AgentResponse(content, null, null, null, true);
    }
}
