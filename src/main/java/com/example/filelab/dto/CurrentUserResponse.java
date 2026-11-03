package com.example.filelab.dto;

import lombok.Getter;

import java.time.LocalDateTime;
@Getter
//不直接传用户user，因为其中有password在里面
public class CurrentUserResponse {

    private Long id;
    private String username;
    private String role;
    private LocalDateTime createTime;

    public CurrentUserResponse(Long id, String username, String role, LocalDateTime createTime) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.createTime = createTime;
    }

}