package com.example.filelab.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@TableName("sys_user")
public class SysUser {

    @TableId
    private Long id;

    private String username;

    private String password;

    private String role;

    private LocalDateTime createTime;

}