package com.bitforum.ai.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.bitforum.ai.agent.AgentType;

/**
 * 工具注册表单元测试（M14）。
 *
 * 重点验证最小权限约束：问答助手可以拿到工具，而审核/运营/推荐 Agent
 * 当前不能拿到任何工具。这条约束在 M16 接入审核 Agent 时尤其关键 ——
 * 审核 Agent 绝不应该拥有"发文章""点赞""关注"这类写能力。
 */
class ToolRegistryTest {

    private final ArticleTools articleTools = new ArticleTools(null, null, null, null);
    private final UserInteractionTools interactionTools =
            new UserInteractionTools(null, null, null, null);

    @Test
    void qaAgentShouldReceiveQueryAndInteractionTools() {
        Object[] tools = ToolRegistry.toolsFor(AgentType.QA, articleTools, interactionTools);

        assertEquals(2, tools.length, "问答助手应拿到文章工具与互动工具两组");
        assertEquals(articleTools, tools[0]);
        assertEquals(interactionTools, tools[1]);
    }

    @Test
    void nonQaAgentsShouldNotReceiveAnyToolYet() {
        for (AgentType type : new AgentType[]{AgentType.MODERATION, AgentType.ANALYST, AgentType.RECOMMEND}) {
            Object[] tools = ToolRegistry.toolsFor(type, articleTools, interactionTools);
            assertEquals(0, tools.length,
                    type + " 当前不应装配任何工具（M16/M17 接入时需显式设计其最小权限工具集）");
        }
    }

    @Test
    void nullTypeShouldReturnEmptyInsteadOfFailing() {
        assertEquals(0, ToolRegistry.toolsFor(null, articleTools, interactionTools).length,
                "类型为空时应安全返回空数组，而不是抛异常打断对话");
    }

    @Test
    void toolRegistryConstantsShouldBeStable() {
        // 工具名会出现在模型看到的 schema 中，改动会影响提示词与历史会话的可复现性
        assertNotNull(AgentType.QA.name());
        assertTrue(AgentType.QA.getDisplayName().contains("问答"));
    }
}
