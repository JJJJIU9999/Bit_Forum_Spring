package com.bitforum.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bitforum.entity.Comment;
import com.bitforum.mapper.CommentMapper;

@Service
public class CommentService {
    @Autowired
    private CommentMapper commentMapper;

    private static final Logger log = LoggerFactory.getLogger(CommentService.class);

    //发表评论
    public void publish(Long userId, Long articleId, String content) {
        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setArticleId(articleId);
        comment.setContent(content);
        comment.setParentCommentId(0L); //0表示“一级评论”
        log.info("发表评论成功,发表评论的用户ID是{}",userId);
        commentMapper.insert(comment);
    }

    //查看某篇文章的所有评论
    public List<Comment> listByArticleId(Long articleId) {
        QueryWrapper<Comment> wrapper = new QueryWrapper<>();
        wrapper.eq("article_id", articleId).orderByAsc("create_time");
        List<Comment> commentList = commentMapper.selectList(wrapper);
        log.info("文章的所有评论如下：{}", commentList);
        return commentList;
    }
}
