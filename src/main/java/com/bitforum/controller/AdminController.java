package com.bitforum.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bitforum.common.Result;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @GetMapping("/health")
    public Result<String> health() {
        // 这个接口只用于验证 /api/admin/** 权限链路；真正的后台业务接口后续再分模块增加。
        return Result.ok("管理员接口访问成功", null);
    }
}
