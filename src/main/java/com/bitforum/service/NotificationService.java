package com.bitforum.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bitforum.entity.Article;
import com.bitforum.entity.Notification;
import com.bitforum.mapper.NotificationMapper;

@Service
public class NotificationService {
    public static final String TYPE_COMMENT = "COMMENT";
    public static final String TYPE_LIKE = "LIKE";
    public static final String TYPE_FAVORITE = "FAVORITE";
    public static final String TYPE_AUDIT_APPROVED = "AUDIT_APPROVED";
    public static final String TYPE_AUDIT_REJECTED = "AUDIT_REJECTED";
    public static final String TYPE_ARTICLE_OFFLINE = "ARTICLE_OFFLINE";

    public static final int READ_STATUS_UNREAD = 0;
    public static final int READ_STATUS_READ = 1;

    @Autowired
    private NotificationMapper notificationMapper;

    public Page<Notification> pageNotifications(Long userId, long pageNum, long pageSize) {
        Page<Notification> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Notification> wrapper = new QueryWrapper<>();
        wrapper.eq("receiver_id", userId).orderByDesc("create_time");
        return notificationMapper.selectPage(page, wrapper);
    }

    public Long countUnread(Long userId) {
        return notificationMapper.selectCount(new QueryWrapper<Notification>()
                .eq("receiver_id", userId)
                .eq("read_status", READ_STATUS_UNREAD));
    }

    public void markRead(Long userId, Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null || !userId.equals(notification.getReceiverId())) {
            throw new RuntimeException("通知不存在或不属于当前用户");
        }
        if (!Integer.valueOf(READ_STATUS_READ).equals(notification.getReadStatus())) {
            notification.setReadStatus(READ_STATUS_READ);
            notificationMapper.updateById(notification);
        }
    }

    public void markAllRead(Long userId) {
        Notification notification = new Notification();
        notification.setReadStatus(READ_STATUS_READ);
        notificationMapper.update(notification, new UpdateWrapper<Notification>()
                .eq("receiver_id", userId)
                .eq("read_status", READ_STATUS_UNREAD));
    }

    @Transactional
    public void notifyComment(Article article, Long senderId, String commentContent) {
        createArticleNotification(
                article,
                senderId,
                TYPE_COMMENT,
                "收到新评论",
                null,
                "用户 " + senderId + " 评论了你的文章《" + safeTitle(article) + "》：" + truncate(commentContent, 180));
    }

    @Transactional
    public void notifyLike(Article article, Long senderId) {
        createArticleNotification(
                article,
                senderId,
                TYPE_LIKE,
                "收到新点赞",
                null,
                "用户 " + senderId + " 点赞了你的文章《" + safeTitle(article) + "》。");
    }

    @Transactional
    public void notifyFavorite(Article article, Long senderId) {
        createArticleNotification(
                article,
                senderId,
                TYPE_FAVORITE,
                "收到新收藏",
                null,
                "用户 " + senderId + " 收藏了你的文章《" + safeTitle(article) + "》。");
    }

    @Transactional
    public void notifyAuditApproved(Article article, Long auditorId, String sourceMessageId) {
        try {
            createArticleNotification(
                    article,
                    auditorId,
                    TYPE_AUDIT_APPROVED,
                    "文章审核通过",
                    sourceMessageId,
                    "你的文章《" + safeTitle(article) + "》已审核通过并发布。");
        } catch (DuplicateKeyException e) {
            // 同一个 MQ messageId 已经落过通知时，重放视为幂等成功。
        }
    }

    @Transactional
    public void notifyAuditRejected(Article article, Long auditorId, String reason) {
        createArticleNotification(
                article,
                auditorId,
                TYPE_AUDIT_REJECTED,
                "文章审核未通过",
                null,
                "你的文章《" + safeTitle(article) + "》审核未通过，原因：" + truncate(reason, 220));
    }

    @Transactional
    public void notifyArticleOffline(Article article, Long auditorId, String reason) {
        createArticleNotification(
                article,
                auditorId,
                TYPE_ARTICLE_OFFLINE,
                "文章已下架",
                null,
                "你的文章《" + safeTitle(article) + "》已被下架，原因：" + truncate(reason, 220));
    }

    private void createArticleNotification(
            Article article,
            Long senderId,
            String type,
            String title,
            String sourceMessageId,
            String content) {
        if (article == null || article.getUserId() == null) {
            return;
        }
        if (senderId != null && article.getUserId().equals(senderId)) {
            return;
        }

        Notification notification = new Notification();
        notification.setReceiverId(article.getUserId());
        notification.setSenderId(senderId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(truncate(content, 500));
        notification.setArticleId(article.getId());
        notification.setSourceMessageId(sourceMessageId);
        notification.setReadStatus(READ_STATUS_UNREAD);
        notificationMapper.insert(notification);
    }

    private String safeTitle(Article article) {
        if (article == null || article.getTitle() == null) {
            return "";
        }
        return truncate(article.getTitle(), 50);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}

