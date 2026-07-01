package com.bitforum.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import com.bitforum.dto.FileUploadResponse;

class FileUploadServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void emptyFileShouldBeRejected() {
        FileUploadService service = new FileUploadService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[0]);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.uploadAvatar(file);
        });

        assertEquals("上传文件不能为空", exception.getMessage());
    }

    @Test
    void nonImageFileShouldBeRejected() {
        FileUploadService service = new FileUploadService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain", "hello".getBytes());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.uploadAvatar(file);
        });

        assertEquals("只允许上传 JPG、PNG 或 WebP 图片", exception.getMessage());
    }

    @Test
    void avatarLargerThanLimitShouldBeRejected() {
        FileUploadService service = new FileUploadService(tempDir.toString());
        byte[] content = new byte[2 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", content);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.uploadAvatar(file);
        });

        assertEquals("上传文件大小超过限制", exception.getMessage());
    }

    @Test
    void articleCoverLargerThanLimitShouldBeRejected() {
        FileUploadService service = new FileUploadService(tempDir.toString());
        byte[] content = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "cover.jpg", "image/jpeg", content);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.uploadArticleCover(file);
        });

        assertEquals("上传文件大小超过限制", exception.getMessage());
    }

    @Test
    void validAvatarShouldBeSavedWithGeneratedName() throws Exception {
        FileUploadService service = new FileUploadService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "..\\avatar.png", "image/png", "png".getBytes());

        FileUploadResponse response = service.uploadAvatar(file);

        assertTrue(response.getUrl().startsWith("/uploads/avatar/"));
        assertTrue(response.getUrl().endsWith(".png"));
        assertEquals("avatar.png", response.getOriginalFilename());
        assertEquals("image/png", response.getContentType());
        assertEquals(3, response.getSize());
        assertFalse(response.getUrl().contains("avatar.png"));

        String filename = response.getUrl().substring(response.getUrl().lastIndexOf('/') + 1);
        assertTrue(Files.exists(tempDir.resolve("avatar").resolve(filename)));
    }

    @Test
    void validArticleCoverShouldReturnCoverUrl() throws Exception {
        FileUploadService service = new FileUploadService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "cover.jpeg", "image/jpeg", "jpg".getBytes());

        FileUploadResponse response = service.uploadArticleCover(file);

        assertTrue(response.getUrl().startsWith("/uploads/article-cover/"));
        assertTrue(response.getUrl().endsWith(".jpg"));

        String filename = response.getUrl().substring(response.getUrl().lastIndexOf('/') + 1);
        assertTrue(Files.exists(tempDir.resolve("article-cover").resolve(filename)));
    }
}
