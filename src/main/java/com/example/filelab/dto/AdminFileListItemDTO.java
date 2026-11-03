package com.example.filelab.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AdminFileListItemDTO {

    private Long id;

    private Long userId;

    private String username;

    private String originalName;

    private Long fileSize;

    private String contentType;

    private LocalDateTime createTime;
}