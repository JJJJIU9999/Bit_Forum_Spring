package com.bitforum.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.bitforum.dto.FileUploadResponse;

@Service
public class FileUploadService {
    private static final long AVATAR_MAX_SIZE = 2L * 1024 * 1024;
    private static final long ARTICLE_COVER_MAX_SIZE = 5L * 1024 * 1024;
    private static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    private final Path uploadRoot;

    public FileUploadService(@Value("${bitforum.upload.root:uploads}") String uploadRoot) {
        this.uploadRoot = Paths.get(uploadRoot).toAbsolutePath().normalize();
    }

    public FileUploadResponse uploadAvatar(MultipartFile file) {
        return uploadImage(file, "avatar", AVATAR_MAX_SIZE);
    }

    public FileUploadResponse uploadArticleCover(MultipartFile file) {
        return uploadImage(file, "article-cover", ARTICLE_COVER_MAX_SIZE);
    }

    private FileUploadResponse uploadImage(MultipartFile file, String folder, long maxSize) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        if (file.getSize() > maxSize) {
            throw new RuntimeException("上传文件大小超过限制");
        }

        String contentType = normalizeContentType(file.getContentType());
        String extension = IMAGE_EXTENSIONS.get(contentType);
        if (extension == null) {
            throw new RuntimeException("只允许上传 JPG、PNG 或 WebP 图片");
        }

        Path targetDir = uploadRoot.resolve(folder).normalize();
        ensureInRoot(targetDir);

        String filename = UUID.randomUUID() + "." + extension;
        Path target = targetDir.resolve(filename).normalize();
        if (!target.startsWith(targetDir)) {
            throw new RuntimeException("文件保存路径不合法");
        }

        try {
            Files.createDirectories(targetDir);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new RuntimeException("文件保存失败");
        }

        FileUploadResponse response = new FileUploadResponse();
        response.setUrl("/uploads/" + folder + "/" + filename);
        response.setOriginalFilename(cleanOriginalFilename(file.getOriginalFilename()));
        response.setSize(file.getSize());
        response.setContentType(contentType);
        return response;
    }

    private void ensureInRoot(Path path) {
        if (!path.startsWith(uploadRoot)) {
            throw new RuntimeException("文件保存目录不合法");
        }
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        return contentType.toLowerCase(Locale.ROOT);
    }

    private String cleanOriginalFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "unknown";
        }
        String normalized = originalFilename.replace('\\', '/');
        int slashIndex = normalized.lastIndexOf('/');
        return slashIndex >= 0 ? normalized.substring(slashIndex + 1) : normalized;
    }
}
