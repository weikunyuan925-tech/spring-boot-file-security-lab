
package com.example.filelab.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class FileStorageConfig {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private Path uploadRoot;

    @Value("${file.quarantine-dir:quarantine}")
    private String quarantineDir;

    private Path quarantineRoot;

    @PostConstruct
    public void init() throws IOException {

        // 1. 初始化上传目录
        uploadRoot = Path.of(uploadDir)
                .toAbsolutePath()
                .normalize();

        // 2. 初始化隔离目录
        quarantineRoot = Path.of(quarantineDir)
                .toAbsolutePath()
                .normalize();

        // 3. 创建目录
        Files.createDirectories(uploadRoot);
        Files.createDirectories(quarantineRoot);

        // 4. 检查目录是否可用
        if (!Files.isDirectory(uploadRoot)
                || !Files.isWritable(uploadRoot)) {
            throw new IllegalStateException("文件上传目录不可用");
        }

        if (!Files.isDirectory(quarantineRoot)
                || !Files.isWritable(quarantineRoot)) {
            throw new IllegalStateException("文件隔离目录不可用");
        }

        // 5. 确保两个目录不相同
        if (uploadRoot.equals(quarantineRoot)) {
            throw new IllegalStateException("上传目录和隔离目录不能相同");
        }

    }

        public Path getUploadRoot() {
            return uploadRoot;
        }

        public Path getQuarantineRoot() {
            return quarantineRoot;
        }

}
