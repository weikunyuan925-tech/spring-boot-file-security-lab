package com.example.filelab.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.filelab.common.Result;
import com.example.filelab.dto.CurrentUserResponse;
import com.example.filelab.dto.UserLoginRequest;
import com.example.filelab.dto.UserRegisterRequest;
import com.example.filelab.entity.SysUser;
import com.example.filelab.mapper.SysUserMapper;
import com.example.filelab.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class SysUserController {

    private final SysUserMapper sysUserMapper;
    private final TokenService tokenService;

    public SysUserController(
            SysUserMapper sysUserMapper,
            TokenService tokenService) {
        this.sysUserMapper = sysUserMapper;
        this.tokenService = tokenService;
    }


    @GetMapping("/list")
    public Result<List<CurrentUserResponse>> list(
            HttpServletRequest request) {

        // 1. 获取拦截器认证后的用户 ID
        Long currentUserId =
                (Long) request.getAttribute("userId");

        if (currentUserId == null) {
            return Result.error(401, "用户未登录");
        }

        // 2. 查询当前登录用户
        SysUser currentUser =
                sysUserMapper.selectById(currentUserId);

        if (currentUser == null) {
            return Result.error(401, "用户不存在或已失效");
        }

        // 3. 检查管理员权限
        if (!"ADMIN".equals(currentUser.getRole())) {
            return Result.error(403, "无权查看用户列表");
        }

        // 4. 仅返回安全 DTO，不直接返回 SysUser 实体
        List<CurrentUserResponse> users =
                sysUserMapper.selectList(null)
                        .stream()
                        .map(user -> new CurrentUserResponse(
                                user.getId(),
                                user.getUsername(),
                                user.getRole()
                        ))
                        .toList();

        return Result.success("获取成功", users);
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

    @PostMapping("/login")
    public Result<Map<String, String>> login(
            @RequestBody UserLoginRequest request) {

        if (request == null
                || request.getUsername() == null
                || request.getUsername().isBlank()
                || request.getPassword() == null
                || request.getPassword().isBlank()) {
            return Result.error(400, "用户名和密码不能为空");
        }

        SysUser user = sysUserMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getUsername,
                                request.getUsername().trim())
        );

        // 不区分用户名错误和密码错误，避免泄露账号是否存在
        if (user == null
                || !user.getPassword().equals(request.getPassword())) {
            return Result.error(401, "用户名或密码错误");
        }

        String token = tokenService.createToken(user.getId());

        return Result.success(
                "登录成功",
                Map.of("token", token)
        );
    }

    @GetMapping("/me")
    public Result<CurrentUserResponse> me(
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");

        SysUser user = sysUserMapper.selectById(userId);

        if (user == null) {
            return Result.error(401, "用户不存在或已失效");
        }

        CurrentUserResponse data = new CurrentUserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole()
        );

        return Result.success("获取成功", data);
    }
}