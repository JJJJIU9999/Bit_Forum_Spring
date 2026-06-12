package com.bitforum.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bitforum.entity.Comment;
import com.bitforum.mapper.ArticleMapper;
import com.bitforum.mapper.CommentMapper;

@SpringBootTest
// 测试结束后自动回滚数据库，避免测试评论或测试文章污染本地数据
@Transactional
public class CommentServiceTest {
    @Autowired
    private CommentService commentService;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private ArticleMapper articleMapper;

    @Test
    void publishShouldFailWhenArticleNotExists() {
        Long userId = 30001L;
        Long notExistsArticle = 99999999L;
        String content = "不存在文章评论测试-" + UUID.randomUUID();

        // 先确认这个 articleId 在数据库里真的不存在，测试前提要明确
        assertNull(articleMapper.selectById(notExistsArticle));

        boolean success = commentService.publish(userId, notExistsArticle, content);

        // CommentService.publish 遇到不存在的文章时应该返回 false，让 Controller 返回业务失败提示
        assertFalse(success);

        // 再查 comment 表，确认没有因为异常场景插入脏评论
        QueryWrapper<Comment> wrapper = new QueryWrapper<>();
        wrapper.eq("article_id", notExistsArticle);
        wrapper.eq("content", content);

        assertEquals(0L, commentMapper.selectCount(wrapper));
    }

    @Test
    void adminShouldDeleteExistingComment() {
        Comment comment = new Comment();
        comment.setArticleId(40001L);
        comment.setUserId(40002L);
        comment.setContent("管理员删除评论测试-" + UUID.randomUUID());
        comment.setParentCommentId(0L);
        commentMapper.insert(comment);

        boolean deleted = commentService.deleteByAdmin(comment.getId());

        assertTrue(deleted);
        // 管理员删除评论后，comment 表中不应该还能查到这条记录。
        assertNull(commentMapper.selectById(comment.getId()));
    }

    @Test
    void adminDeleteShouldReturnFalseWhenCommentNotExists() {
        Long notExistsCommentId = 99999999L;

        assertNull(commentMapper.selectById(notExistsCommentId));

        boolean deleted = commentService.deleteByAdmin(notExistsCommentId);

        assertFalse(deleted);
    }
}
