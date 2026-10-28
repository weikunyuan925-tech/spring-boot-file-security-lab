package com.example.filelab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.filelab.config.FileStorageConfig;
import com.example.filelab.dto.FileListItemDTO;
import com.example.filelab.entity.FileInfo;
import com.example.filelab.mapper.FileInfoMapper;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class FileService {

    private final FileInfoMapper fileInfoMapper;
    private final FileStorageConfig fileStorageConfig;

    public FileService(FileInfoMapper fileInfoMapper, FileStorageConfig fileStorageConfig) {
        this.fileInfoMapper = fileInfoMapper;
        this.fileStorageConfig = fileStorageConfig;
    }

    public List<FileListItemDTO> listByUserId(Long userId) {

        // 只查询当前用户拥有的文件
        LambdaQueryWrapper<FileInfo> query = new LambdaQueryWrapper<>();

        query.eq(FileInfo::getUserId, userId)//核心
                .orderByDesc(FileInfo::getCreateTime);

        List<FileInfo> files = fileInfoMapper.selectList(query);

        // 转换成只包含必要字段的 DTO
        return files.stream().map(file -> new FileListItemDTO(file.getId(), file.getOriginalName(), file.getFileSize(), file.getCreateTime())).toList();
    }

    public FileInfo getOwnedFile(Long fileId, Long userId) {

        LambdaQueryWrapper<FileInfo> query = new LambdaQueryWrapper<>();
        //SQL 查询会同时限制文件 ID 和当前用户 ID。
        query.eq(FileInfo::getId, fileId).eq(FileInfo::getUserId, userId);

        return fileInfoMapper.selectOne(query);
    }

    public int deleteOwnedFile(Long fileId, Long userId) {

        if (fileId == null || userId == null) {
            return 0;
        }

        LambdaQueryWrapper<FileInfo> query = new LambdaQueryWrapper<>();

        query.eq(FileInfo::getId, fileId).eq(FileInfo::getUserId, userId);

        return fileInfoMapper.delete(query);
    }


    /**
     * 保存上传文件及其数据库记录。
     * 数据库保存失败时，尝试清理已经写入磁盘的文件。
     */
    public FileInfo storeUploadedFile(
            MultipartFile file,
            Long userId,
            String originalName,
            String extension,
            String detectedMime) throws FileUploadException {

        Path savedPath = null;

        try {
            // 1. 获取已经规范化的上传根目录
            Path uploadRoot = fileStorageConfig.getUploadRoot()
                    .toAbsolutePath()
                    .normalize();

            Files.createDirectories(uploadRoot);

            // 2. 使用 UUID 生成服务器端文件名
            String storedName = UUID.randomUUID() + extension;

            savedPath = uploadRoot.resolve(storedName).normalize();

            // 防御性检查：保存路径必须在上传根目录内
            if (!savedPath.startsWith(uploadRoot)) {
                throw new IllegalStateException("非法文件保存路径");
            }

            // 3. 先写入磁盘
            file.transferTo(savedPath);

            // 4. 再构建数据库记录
            FileInfo info = new FileInfo();
            info.setUserId(userId);
            info.setOriginalName(originalName);
            info.setStoredName(storedName);
            info.setFilePath(savedPath.toString());
            info.setFileSize(file.getSize());
            info.setContentType(detectedMime);
            info.setCreateTime(LocalDateTime.now());

            // 5. 保存数据库记录
            int rows = fileInfoMapper.insert(info);

            if (rows != 1) {
                throw new IllegalStateException("文件信息保存失败");
            }

            // 6. 返回已保存的文件信息
            return info;

        } catch (Exception e) {
            // 文件写入或数据库保存失败，尝试删除本次生成的文件
            if (savedPath != null) {
                try {
                    Files.deleteIfExists(savedPath);
                } catch (IOException cleanupException) {
                    // 不能让清理异常覆盖最初的保存异常
                    e.addSuppressed(cleanupException);
                }
            }

            throw new FileUploadException("文件保存失败", e);
        }
    }


}