package com.example.filelab.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;


@Configuration
public class FileStorageConfig {

    // 上传目录配置
    @Setter
    @Value("${file.upload-dir}")
    private String uploadDir;

    // 返回已经解析过的真实上传目录
    @Getter
    private Path uploadRoot;

    // 隔离目录配置
    @Setter
    @Value("${file.quarantine-dir:quarantine}")
    private String quarantineDir;

    // 返回已经解析过的真实隔离目录
    @Getter
    private Path quarantineRoot;

    @PostConstruct
    public void init() {

        try {
            // 1. 检查配置
            if (uploadDir == null || uploadDir.isBlank()) {
                throw new IllegalStateException("文件上传目录配置不能为空");
            }

            if (quarantineDir == null || quarantineDir.isBlank()) {
                throw new IllegalStateException("文件隔离目录配置不能为空");
            }

            // 2. 将配置字符串转换为绝对路径并规范化
            Path configuredUploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();

            Path configuredQuarantineRoot = Path.of(quarantineDir).toAbsolutePath().normalize();

            // 3. 创建目录
            Files.createDirectories(configuredUploadRoot);

            Files.createDirectories(configuredQuarantineRoot);

            // 4. 解析真实路径
            // 如果目录自身是符号链接，这里会解析到实际目标目录
            this.uploadRoot = configuredUploadRoot.toRealPath();

            this.quarantineRoot = configuredQuarantineRoot.toRealPath();

            // 5. 检查上传目录是否可用
            if (!Files.isDirectory(uploadRoot) || !Files.isWritable(uploadRoot)) {

                throw new IllegalStateException("文件上传目录不可用");
            }

            // 6. 检查隔离目录是否可用
            if (!Files.isDirectory(quarantineRoot) || !Files.isWritable(quarantineRoot)) {

                throw new IllegalStateException("文件隔离目录不可用");
            }

            // 7. 检查两个目录不能相同
            if (uploadRoot.equals(quarantineRoot)) {

                throw new IllegalStateException("上传目录和隔离目录不能相同");
            }

            // 8. 确保隔离目录不能位于上传目录内部
            if (quarantineRoot.startsWith(uploadRoot)) {

                throw new IllegalStateException("隔离目录不能位于上传目录内部");
            }

        } catch (IOException | InvalidPathException e) {

            throw new IllegalStateException("文件存储目录初始化失败", e);
        }
    }

}


