package com.bitforum.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bitforum.entity.User;
import com.bitforum.service.UserService;
import com.bitforum.util.JwtUtil;

@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserService userService;

    @Test
    void adminApiShouldRejectRequestWithoutToken() throws Exception {
        // 管理员接口第一层风险是未登录访问，这里验证没有 Authorization 时直接被拦截。
        mockMvc.perform(get("/api/admin/health"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("请先登录"));
    }

    @Test
    void adminApiShouldRejectNormalUser() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole("USER");
        user.setStatus(1);

        when(jwtUtil.getUserId("user-token")).thenReturn(1L);
        when(userService.findById(1L)).thenReturn(user);

        // 普通用户即使有合法 token，也不能访问 /api/admin/**，权限必须由后端拦截。
        mockMvc.perform(get("/api/admin/health")
                .header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无管理员权限"));
    }

    @Test
    void adminApiShouldRejectDisabledAdmin() throws Exception {
        User admin = new User();
        admin.setId(2L);
        admin.setRole(UserService.ROLE_ADMIN);
        admin.setStatus(0);

        when(jwtUtil.getUserId("disabled-admin-token")).thenReturn(2L);
        when(userService.findById(2L)).thenReturn(admin);

        // 即使 role 是 ADMIN，status=0 也不能放行，避免被禁用账号继续访问后台。
        mockMvc.perform(get("/api/admin/health")
                .header("Authorization", "Bearer disabled-admin-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("账号不可用"));
    }

    @Test
    void adminApiShouldAllowEnabledAdmin() throws Exception {
        User admin = new User();
        admin.setId(3L);
        admin.setRole(UserService.ROLE_ADMIN);
        admin.setStatus(1);

        when(jwtUtil.getUserId("admin-token")).thenReturn(3L);
        when(userService.findById(3L)).thenReturn(admin);

        mockMvc.perform(get("/api/admin/health")
                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("管理员接口访问成功"));
    }
}
