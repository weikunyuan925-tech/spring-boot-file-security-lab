package com.example.filelab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.filelab.entity.FileInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileInfoMapper extends BaseMapper<FileInfo> {
}