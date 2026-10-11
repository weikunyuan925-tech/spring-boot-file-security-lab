package com.example.filelab.controller;

import com.example.filelab.entity.SysUser;
import com.example.filelab.mapper.SysUserMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class SysUserController {

    private final SysUserMapper sysUserMapper;

    public SysUserController(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @GetMapping("/list")
    public List<SysUser> list() {
        return sysUserMapper.selectList(null);
    }
}