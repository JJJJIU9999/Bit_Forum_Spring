package com.bitforum.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bitforum.entity.Article;

@Mapper
public interface ArticleMapper extends BaseMapper<Article>{
    
}
