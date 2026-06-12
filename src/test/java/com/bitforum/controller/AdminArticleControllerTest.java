package com.bitforum.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bitforum.entity.Article;
import com.bitforum.entity.User;
import com.bitforum.service.ArticleService;
import com.bitforum.service.UserService;
import com.bitforum.util.JwtUtil;

@SpringBootTest
@AutoConfigureMockMvc
class AdminArticleControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ArticleService articleService;

    @Test
    void adminShouldPageArticles() throws Exception {
        mockAdmin("admin-token", 10L);
        when(articleService.pageArticles(1, 10)).thenReturn(new Page<Article>(1, 10));

        // 管理员文章分页接口只负责转发分页查询，权限判断由 AdminInterceptor 先完成。
        mockMvc.perform(get("/api/admin/article/page")
                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("管理员文章分页查询成功"));
    }

    @Test
    void adminShouldDeleteAnyArticle() throws Exception {
        mockAdmin("admin-token", 10L);
        when(articleService.deleteByAdmin(1L)).thenReturn(true);

        // 管理员删除接口不再判断文章作者是谁，只要管理员身份通过就调用后台删除逻辑。
        mockMvc.perform(delete("/api/admin/article/delete")
                .param("articleId", "1")
                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("管理员删除文章成功"));
    }

    @Test
    void adminDeleteShouldFailWhenArticleNotExists() throws Exception {
        mockAdmin("admin-token", 10L);
        when(articleService.deleteByAdmin(404L)).thenReturn(false);

        mockMvc.perform(delete("/api/admin/article/delete")
                .param("articleId", "404")
                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("文章不存在"));
    }

    @Test
    void normalUserShouldNotDeleteArticleThroughAdminApi() throws Exception {
        User user = new User();
        user.setId(20L);
        user.setRole("USER");
        user.setStatus(UserService.STATUS_ENABLED);

        when(jwtUtil.getUserId("user-token")).thenReturn(20L);
        when(userService.findById(20L)).thenReturn(user);

        // 即使普通用户知道管理员删除接口地址，也会先被 AdminInterceptor 拦住。
        mockMvc.perform(delete("/api/admin/article/delete")
                .param("articleId", "1")
                .header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("无管理员权限"));
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
