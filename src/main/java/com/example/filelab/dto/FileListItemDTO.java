package com.example.filelab.dto;

import java.time.LocalDateTime;

public record FileListItemDTO(Long id, String fileName, Long fileSize, LocalDateTime createTime) {
}