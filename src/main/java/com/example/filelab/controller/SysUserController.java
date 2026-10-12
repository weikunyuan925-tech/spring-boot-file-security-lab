package com.example.filelab.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.filelab.common.Result;
import com.example.filelab.dto.UserRegisterRequest;
import com.example.filelab.entity.SysUser;
import com.example.filelab.mapper.SysUserMapper;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/register")
    public Result<Void> register(@RequestBody UserRegisterRequest request) {

        // 1. 校验请求体
        if (request == null) {
            return Result.error(400, "请求参数不能为空");
        }

        // 2. 校验用户名
        if (request.getUsername() == null
                || request.getUsername().trim().isBlank()) {
            return Result.error(400, "用户名不能为空");
        }

        // 3. 校验密码
        if (request.getPassword() == null
                || request.getPassword().isBlank()) {
            return Result.error(400, "密码不能为空");
        }

        String username = request.getUsername().trim();

        // 4. 查询用户名是否已经存在
        SysUser existingUser = sysUserMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getUsername, username)
        );

        if (existingUser != null) {
            return Result.error(409, "用户名已存在");
        }

        // 5. 创建用户
        SysUser user = new SysUser();
        user.setUsername(username);

        // 先故意明文保存
        // 后续安全审计阶段改成 BCrypt
        user.setPassword(request.getPassword());

        user.setRole("USER");

        // 6. 保存到数据库
        sysUserMapper.insert(user);

        // 7. 返回结果
        return Result.success("注册成功");
    }

}