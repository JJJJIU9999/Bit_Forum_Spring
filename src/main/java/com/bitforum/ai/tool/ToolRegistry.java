package com.bitforum.ai.tool;

import com.bitforum.ai.agent.AgentType;

/**
 * 工具注册表（M14）。
 *
 * 职责：按 Agent 类型装配可见的工具集合。
 *
 * 为什么需要按类型装配而不是全部暴露：M16 起会有审核 Agent 与运营 Agent，
 * 它们绝不能拿到"发文章""点赞""关注"这类写工具，否则一旦模型判断失误就会篡改业务数据。
 * 最小权限是 Agent 设计里的硬约束，不能靠提示词自觉。
 */
public final class ToolRegistry {

    private ToolRegistry() {
    }

    /**
     * 返回指定 Agent 可用的工具对象数组。
     *
     * 当前（M14）只有问答助手，它需要完整的查询与互动能力：
     * 查询类工具（搜索、详情、板块、热榜、关注统计）可直接调用；
     * 写操作类工具（收藏、点赞、关注、建草稿）在工具描述中要求先与用户确认。
     */
    public static Object[] toolsFor(AgentType type, ArticleTools articleTools,
                                    UserInteractionTools interactionTools) {
        if (type == null) {
            return new Object[0];
        }
        return switch (type) {
            case QA -> new Object[]{
                    articleTools,      // searchArticles / getArticleDetail / getHotArticles
                                       // listCategories / createDraftArticle
                    interactionTools,  // favorite / unfavorite / like / unlike
                                       // follow / unfollow / getFollowStats
            };
            // M16 接入审核 Agent、M17 接入运营与推荐 Agent 时在此按需装配，
            // 审核与运营一律不给写工具。
            case MODERATION, ANALYST, RECOMMEND -> new Object[0];
        };
    }
}
