package com.bitforum.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bitforum.entity.Article;
import com.bitforum.entity.Comment;
import com.bitforum.mapper.ArticleMapper;
import com.bitforum.mapper.CommentMapper;

@Service
public class CommentService {
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private ArticleMapper articleMapper;

    private static final Logger log = LoggerFactory.getLogger(CommentService.class);

    //发表评论
    public boolean publish(Long userId, Long articleId, String content) {
        // 评论是文章的从属数据，插入评论前必须先确认文章存在
        // 这里直接用 ArticleMapper 查询，避免 CommentService 和 ArticleService 互相注入形成循环依赖
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            // 这里先返回 false，让 Controller 决定给前端什么业务提示
            return false;
        }
        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setArticleId(articleId);
        comment.setContent(content);
        comment.setParentCommentId(0L); //0表示“一级评论”
        log.info("发表评论成功,发表评论的用户ID是{}",userId);
        commentMapper.insert(comment);
        // 插入成功后返回 true，表示 Controller 可以返回“评论发布成功”
        return true;
    }

    //查看某篇文章的所有评论
    public List<Comment> listByArticleId(Long articleId) {
        QueryWrapper<Comment> wrapper = new QueryWrapper<>();
        wrapper.eq("article_id", articleId).orderByAsc("create_time");
        List<Comment> commentList = commentMapper.selectList(wrapper);
        log.info("文章的所有评论如下：{}", commentList);
        return commentList;
    }

    public void deleteByArticleId(Long articleId) {
        // 删除文章时，先按 article_id 清理这篇文章下的所有评论
        QueryWrapper<Comment> wrapper = new QueryWrapper<>();
        wrapper.eq("article_id", articleId);
        commentMapper.delete(wrapper);
    }
}
