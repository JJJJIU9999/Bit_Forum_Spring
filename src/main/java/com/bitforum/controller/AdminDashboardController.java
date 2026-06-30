package com.bitforum.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bitforum.common.Result;
import com.bitforum.dto.AdminDashboardSummaryResponse;
import com.bitforum.service.AdminDashboardService;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {
    @Autowired
    private AdminDashboardService adminDashboardService;

    @GetMapping("/summary")
    public Result<AdminDashboardSummaryResponse> summary() {
        try {
            return Result.ok("管理员数据看板统计成功", adminDashboardService.summary());
        } catch (RuntimeException e) {
            return Result.fail("管理员数据看板统计失败：" + e.getMessage());
        }
    }
}
