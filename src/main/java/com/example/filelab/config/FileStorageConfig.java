
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

    @PostConstruct
    public void init() throws IOException {
        uploadRoot = Path.of(uploadDir)
                .toAbsolutePath()
                .normalize();

        Files.createDirectories(uploadRoot);

        if (!Files.isDirectory(uploadRoot)
                || !Files.isWritable(uploadRoot)) {
            throw new IllegalStateException(
                    "文件上传目录不可用"
            );
        }
    }

    public Path getUploadRoot() {
        return uploadRoot;
    }
}
