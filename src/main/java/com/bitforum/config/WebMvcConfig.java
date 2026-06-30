package com.bitforum.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.bitforum.interceptor.AdminInterceptor;
import com.bitforum.interceptor.LoginInterceptor;

//WebMvcConfigurer 是 Spring MVC 留给你的一个"自定义设置的入口"。实现它，重写里面的方法，就能改 Spring MVC 的默认行为。
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    @Autowired
    private LoginInterceptor loginInterceptor;
    @Autowired
    private AdminInterceptor adminInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/api/admin/**");

        registry.addInterceptor(loginInterceptor)
                .addPathPatterns(
                        "/api/article/publish",
                        "/api/article/draft",
                        "/api/article/submit",
                        "/api/article/favorite",
                        "/api/article/like",
                        "/api/article/unlike",
                        "/api/article/update",
                        "/api/article/delete",
                        "/api/user/articles",
                        "/api/user/favorites",
                        "/api/user/notifications",
                        "/api/user/notifications/**",
                        "/api/user/reports",
                        "/api/user/reports/**",
                        "/api/comment/publish"
                );   
    }
}
