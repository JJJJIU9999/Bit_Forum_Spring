package com.bitforum.ai.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bitforum.ai.entity.AiConversation;

@Mapper
public interface AiConversationMapper extends BaseMapper<AiConversation> {
}
