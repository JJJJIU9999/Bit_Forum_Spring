package com.bitforum.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bitforum.entity.Article;

@Mapper
public interface ArticleMapper extends BaseMapper<Article>{
    @Select("""
            SELECT a.*
            FROM article_favorite af
            INNER JOIN article a ON a.id = af.article_id
            WHERE af.user_id = #{userId}
              AND a.status = #{status}
            ORDER BY af.create_time DESC
            """)
    Page<Article> selectFavoriteArticles(
            Page<Article> page,
            @Param("userId") Long userId,
            @Param("status") String status);
}
