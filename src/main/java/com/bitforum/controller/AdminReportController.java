package com.bitforum.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bitforum.common.Result;
import com.bitforum.dto.ReportHandleRequest;
import com.bitforum.entity.ContentReport;
import com.bitforum.service.ContentReportService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {
    @Autowired
    private ContentReportService contentReportService;

    @GetMapping
    public Result<Page<ContentReport>> pageReports(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(defaultValue = "PENDING") String status) {
        try {
            Page<ContentReport> reportPage = contentReportService.pageAdminReports(pageNum, pageSize, status);
            return Result.ok("管理员举报分页查询成功", reportPage);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping("/resolve")
    public Result<String> resolve(
            @RequestAttribute("userId") Long handlerId,
            @Valid @RequestBody ReportHandleRequest request) {
        try {
            contentReportService.resolve(request.getReportId(), handlerId, request.getHandleResult());
            return Result.ok("举报处理成功", null);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping("/reject")
    public Result<String> reject(
            @RequestAttribute("userId") Long handlerId,
            @Valid @RequestBody ReportHandleRequest request) {
        try {
            contentReportService.reject(request.getReportId(), handlerId, request.getHandleResult());
            return Result.ok("举报驳回成功", null);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }
}

