package com.bitforum.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bitforum.entity.User;
import com.bitforum.mapper.UserMapper;

@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    public User register(String username, String password) {
        log.info("注册请求：用户名={}",username);
        // 1. 查重：用 QueryWrapper 替代手写 SQL
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username); //WHERE 1.username=2.username
        User existing = userMapper.selectOne(wrapper);
        if (existing != null) {
            log.warn("注册失败：用户名已存在({})", username);
            return null;
        }
        // 2. 保存：insert 是 BaseMapper 自带的
        User user = new User();
        user.setUsername(username);
        // user.setPassword(password);
        //存加密密码
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        user.setPassword(encoder.encode(password)); //存加密后的密文
        userMapper.insert(user);
        log.info("注册成功：用户ID={}",user.getId());
        return user;
    }
    
    public User login(String username, String password) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        User user = userMapper.selectOne(wrapper);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (user != null && encoder.matches(password, user.getPassword())) {
            log.info("登录成功！用户的名称是{}", username);
            return user;
        }
        log.info("登录失败，请重新确认用户名和密码！");
        return null;
    }
}
