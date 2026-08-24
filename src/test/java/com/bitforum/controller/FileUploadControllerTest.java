package com.bitforum.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import com.bitforum.dto.FileUploadResponse;
import com.bitforum.entity.User;
import com.bitforum.service.FileUploadService;
import com.bitforum.service.UserService;
import com.bitforum.util.JwtUtil;

@SpringBootTest
@AutoConfigureMockMvc
class FileUploadControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtUtil jwtUtil;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private FileUploadService fileUploadService;

    @Test
    void avatarUploadShouldRequireLogin() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "png".getBytes());

        mockMvc.perform(multipart("/api/upload/avatar").file(file))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        verify(fileUploadService, never()).uploadAvatar(any(MultipartFile.class));
    }

    @Test
    void avatarUploadShouldReturnFileInfo() throws Exception {
        mockEnabledUser("user-token", 1L);
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "png".getBytes());
        when(fileUploadService.uploadAvatar(any(MultipartFile.class))).thenReturn(uploadResponse("/uploads/avatar/mock.png"));

        mockMvc.perform(multipart("/api/upload/avatar")
                .file(file)
                .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("头像上传成功"))
                .andExpect(jsonPath("$.data.url").value("/uploads/avatar/mock.png"));
    }

    @Test
    void articleCoverUploadShouldReturnFileInfo() throws Exception {
        mockEnabledUser("user-token", 2L);
        MockMultipartFile file = new MockMultipartFile("file", "cover.jpg", "image/jpeg", "jpg".getBytes());
        when(fileUploadService.uploadArticleCover(any(MultipartFile.class))).thenReturn(uploadResponse("/uploads/article-cover/mock.jpg"));

        mockMvc.perform(multipart("/api/upload/article-cover")
                .file(file)
                .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("文章封面上传成功"))
                .andExpect(jsonPath("$.data.url").value("/uploads/article-cover/mock.jpg"));
    }

    @Test
    void serviceBusinessErrorShouldReturnResultFail() throws Exception {
        mockEnabledUser("user-token", 3L);
        MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain", "txt".getBytes());
        when(fileUploadService.uploadAvatar(any(MultipartFile.class))).thenThrow(new RuntimeException("只允许上传 JPG、PNG 或 WebP 图片"));

        mockMvc.perform(multipart("/api/upload/avatar")
                .file(file)
                .header("Authorization", "Bearer user-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("只允许上传 JPG、PNG 或 WebP 图片"));
    }

    private FileUploadResponse uploadResponse(String url) {
        FileUploadResponse response = new FileUploadResponse();
        response.setUrl(url);
        response.setOriginalFilename("mock.png");
        response.setSize(3);
        response.setContentType("image/png");
        return response;
    }

    private void mockEnabledUser(String token, Long userId) {
        User user = new User();
        user.setId(userId);
        user.setStatus(UserService.STATUS_ENABLED);

        when(jwtUtil.getUserId(token)).thenReturn(userId);
        when(userService.findById(userId)).thenReturn(user);
    }
}
