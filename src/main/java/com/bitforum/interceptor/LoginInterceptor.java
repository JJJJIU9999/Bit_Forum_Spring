package com.bitforum.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.bitforum.util.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class LoginInterceptor implements HandlerInterceptor{
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,Object handler)throws Exception {
        //1、取Authorization头
        String authHeader = request.getHeader("Authorization");

        //2、为空格或格式不对 -> 401
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"请先登录\"}");
            return false;
        }

        //3、截取 "Bearer" 后面的Token
        String token = authHeader.substring(7);

        //4、验证Token(篡改、过期都会抛异常)
        try{
            Long userId = JwtUtil.getUserId(token); // 用刚写的工具类解析
            request.setAttribute("userId", userId); //把userId暂存起来，Controller里能拿到
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token无效或已过期\"}");
            return false;
        }
    }
}
