package com.bitforum.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.bitforum.common.Result;
import com.bitforum.dto.FileUploadResponse;
import com.bitforum.service.FileUploadService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/upload")
@Tag(name = "文件上传", description = "头像和文章封面本地图片上传")
@SecurityRequirement(name = "bearerAuth")
public class FileUploadController {
    @Autowired
    private FileUploadService fileUploadService;

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传头像图片", description = "需要登录，支持 JPG、PNG、WebP，最大 2MB")
    public Result<FileUploadResponse> uploadAvatar(
            @Parameter(description = "头像图片文件") @RequestParam("file") MultipartFile file) {
        try {
            return Result.ok("头像上传成功", fileUploadService.uploadAvatar(file));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping(value = "/article-cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传文章封面图片", description = "需要登录，支持 JPG、PNG、WebP，最大 5MB")
    public Result<FileUploadResponse> uploadArticleCover(
            @Parameter(description = "文章封面图片文件") @RequestParam("file") MultipartFile file) {
        try {
            return Result.ok("文章封面上传成功", fileUploadService.uploadArticleCover(file));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }
}
