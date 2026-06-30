package com.bitforum.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bitforum.common.Result;
import com.bitforum.entity.Notification;
import com.bitforum.service.NotificationService;

@RestController
@RequestMapping("/api/user/notifications")
public class UserNotificationController {
    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public Result<Page<Notification>> pageNotifications(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        Page<Notification> page = notificationService.pageNotifications(userId, pageNum, pageSize);
        return Result.ok("通知分页查询成功", page);
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount(@RequestAttribute("userId") Long userId) {
        return Result.ok("未读通知数量查询成功", notificationService.countUnread(userId));
    }

    @PutMapping("/read")
    public Result<String> markRead(
            @RequestAttribute("userId") Long userId,
            @RequestParam Long notificationId) {
        try {
            notificationService.markRead(userId, notificationId);
            return Result.ok("通知已读成功", null);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping("/read-all")
    public Result<String> markAllRead(@RequestAttribute("userId") Long userId) {
        notificationService.markAllRead(userId);
        return Result.ok("全部通知已读成功", null);
    }
}

