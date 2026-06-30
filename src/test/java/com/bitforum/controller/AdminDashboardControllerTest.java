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

import com.bitforum.dto.AdminDashboardSummaryResponse;
import com.bitforum.dto.DashboardUserStats;
import com.bitforum.entity.User;
import com.bitforum.service.AdminDashboardService;
import com.bitforum.service.UserService;
import com.bitforum.util.JwtUtil;

@SpringBootTest
@AutoConfigureMockMvc
class AdminDashboardControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private JwtUtil jwtUtil;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private AdminDashboardService adminDashboardService;

    @Test
    void adminShouldGetDashboardSummary() throws Exception {
        mockAdmin("admin-token", 76001L);

        DashboardUserStats userStats = new DashboardUserStats();
        userStats.setTotal(3L);
        userStats.setNormalUsers(2L);
        userStats.setAdmins(1L);

        AdminDashboardSummaryResponse response = new AdminDashboardSummaryResponse();
        response.setUserStats(userStats);
        when(adminDashboardService.summary()).thenReturn(response);

        mockMvc.perform(get("/api/admin/dashboard/summary")
                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("管理员数据看板统计成功"))
                .andExpect(jsonPath("$.data.userStats.total").value(3))
                .andExpect(jsonPath("$.data.userStats.normalUsers").value(2))
                .andExpect(jsonPath("$.data.userStats.admins").value(1))
                .andExpect(jsonPath("$.data.userStats.password").doesNotExist());
    }

    @Test
    void normalUserShouldNotAccessDashboardSummary() throws Exception {
        User user = new User();
        user.setId(76002L);
        user.setRole("USER");
        user.setStatus(UserService.STATUS_ENABLED);

        when(jwtUtil.getUserId("user-token")).thenReturn(76002L);
        when(userService.findById(76002L)).thenReturn(user);

        mockMvc.perform(get("/api/admin/dashboard/summary")
                .header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("无管理员权限"));
    }

    @Test
    void anonymousUserShouldNotAccessDashboardSummary() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("请先登录"));
    }

    private void mockAdmin(String token, Long userId) {
        User admin = new User();
        admin.setId(userId);
        admin.setRole(UserService.ROLE_ADMIN);
        admin.setStatus(UserService.STATUS_ENABLED);

        when(jwtUtil.getUserId(token)).thenReturn(userId);
        when(userService.findById(userId)).thenReturn(admin);
    }
}
