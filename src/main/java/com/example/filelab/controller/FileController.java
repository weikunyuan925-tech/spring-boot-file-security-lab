package com.example.filelab.controller;

import com.example.filelab.common.Result;
import com.example.filelab.dto.FileListItemDTO;
import com.example.filelab.entity.FileInfo;
import com.example.filelab.mapper.FileInfoMapper;
import com.example.filelab.service.FileService;
import com.example.filelab.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/file")
public class FileController {

    private final FileInfoMapper fileInfoMapper;
    private final FileService fileService;
    private final TokenService tokenService;


    public FileController(
            FileInfoMapper fileInfoMapper, FileService fileService,
            TokenService tokenService) {
        this.fileInfoMapper = fileInfoMapper;
        this.fileService = fileService;
        this.tokenService = tokenService;
    }

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;


    @GetMapping("/list")
    public ResponseEntity<Result<List<FileListItemDTO>>> listFiles(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization) {

        // 1. 检查 Authorization 请求头
        if (authorization == null
                || !authorization.startsWith("Bearer ")) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Result.<List<FileListItemDTO>>failure(
                            401, "缺少或无效的Token"));
        }

        // 2. 提取 Token
        String token = authorization.substring(7).trim();

        if (token.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Result.<List<FileListItemDTO>>failure(
                            401, "缺少或无效的Token"));
        }

        // 3. 通过服务器端 Token 存储识别当前用户
        Long userId = tokenService.getUserId(token);

        if (userId == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Result.<List<FileListItemDTO>>failure(
                            401, "缺少或无效的Token"));
        }

        // 4. 只查询当前用户的文件
        List<FileListItemDTO> files =
                fileService.listByUserId(userId);

        // 5. 返回结果
        return ResponseEntity.ok(Result.success(files));
    }

    @PostMapping("/upload")
    public Result<?> upload(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {

        if (file.isEmpty()) {
            return Result.error(400, "请选择要上传的文件");
        }

        // TokenInterceptor 验证通过后，应该把 userId 放入 request
        Long userId = (Long) request.getAttribute("userId");

        if (userId == null) {
            return Result.error(401, "未登录或 Token 无效");
        }

        Path savedPath = null;

        try {
            // 创建上传目录
            Path directory = Paths.get(uploadDir)
                    .toAbsolutePath()
                    .normalize();

            Files.createDirectories(directory);

            // 使用随机文件名，避免用户通过原始文件名控制保存路径
            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isBlank()) {
                originalName = "unknown";
            }

            String extension = "";
            int dotIndex = originalName.lastIndexOf('.');
            if (dotIndex >= 0) {
                String candidate = originalName.substring(dotIndex);
                if (candidate.matches("\\.[a-zA-Z0-9]{1,10}")) {
                    extension = candidate;
                }
            }

            String storedName = UUID.randomUUID() + extension;
            savedPath = directory.resolve(storedName).normalize();

            // 先保存实际文件到磁盘
            file.transferTo(savedPath);

            // 再保存文件信息到数据库
            FileInfo info = new FileInfo();
            info.setUserId(userId);
            info.setOriginalName(originalName);
            info.setStoredName(storedName);
            info.setFilePath(savedPath.toString());
            info.setFileSize(file.getSize());
            info.setContentType(file.getContentType());
            info.setCreateTime(LocalDateTime.now());

            int rows = fileInfoMapper.insert(info);

            if (rows != 1) {
                Files.deleteIfExists(savedPath);
                return Result.error(500, "文件信息保存失败");
            }

            return Result.success("上传成功", Map.of(
                    "id", info.getId(),
                    "originalName", info.getOriginalName(),
                    "storedName", info.getStoredName(),
                    "fileSize", info.getFileSize()
            ));

        } catch (IOException e) {
            // 如果保存过程中发生 IO 错误，尝试清理已创建的文件
            if (savedPath != null) {
                try {
                    Files.deleteIfExists(savedPath);
                } catch (IOException ignored) {
                    // 保留原始异常，不覆盖它
                }
            }
            return Result.error(500, "文件保存失败");
        }
    }

}