package com.example.filelab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.filelab.dto.FileListItemDTO;
import com.example.filelab.entity.FileInfo;
import com.example.filelab.mapper.FileInfoMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FileService {

    private final FileInfoMapper fileInfoMapper;

    public FileService(FileInfoMapper fileInfoMapper) {
        this.fileInfoMapper = fileInfoMapper;
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

}