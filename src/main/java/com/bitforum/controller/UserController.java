package com.bitforum.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bitforum.common.Result;
import com.bitforum.entity.User;
import com.bitforum.service.UserService;
import com.bitforum.util.JwtUtil;

import org.springframework.web.bind.annotation.PostMapping;

@RestController                 // ← 声明这是一个 REST 控制器
@RequestMapping("/api/user")    // ← 这个 Controller 下所有接口都以 /api/user 开头

public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/register")       // ← POST /api/user/register
    public Result<User> register(@RequestParam String username, @RequestParam String password) {
        User user = userService.register(username, password);
        if (user == null) {
            return Result.fail("注册失败，用户名已存在");
        }
        return Result.ok("注册成功", user);
    }


    @PostMapping("/login")          // ← POST /api/user/login
    public Result<Map<String,Object>> login(@RequestParam String username, @RequestParam String password) {
        User user = userService.login(username, password);
        if (user == null) {
            return Result.fail("登录失败，请重新输入用户名或密码");
        }
        //登录成功 -->生成Token
        String token = JwtUtil.generateToken(user.getId(), user.getUsername());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        return Result.ok("登录成功！", data);
    }
    
}
