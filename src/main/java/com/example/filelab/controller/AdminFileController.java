
package com.example.filelab.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.filelab.common.Result;
import com.example.filelab.dto.AdminFileListItemDTO;
import com.example.filelab.entity.FileInfo;
import com.example.filelab.entity.SysUser;
import com.example.filelab.mapper.SysUserMapper;
import com.example.filelab.service.FileService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/files")
public class AdminFileController {

    private final SysUserMapper sysUserMapper;
    private final FileService fileService;

    public AdminFileController(
            SysUserMapper sysUserMapper,
            FileService fileService) {

        this.sysUserMapper = sysUserMapper;
        this.fileService = fileService;
    }

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestAttribute("userId") Long currentUserId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {

        // 1. 查询当前登录用户
        SysUser currentUser =
                sysUserMapper.selectById(currentUserId);

        if (currentUser == null) {
            return Result.error(401, "用户不存在或已失效");
        }

        // 2. 检查管理员权限
        if (!"ADMIN".equals(currentUser.getRole())) {
            return Result.error(403, "无权查看文件管理列表");
        }

        // 3. 校验分页参数
        if (page < 1 || size < 1 || size > 100) {
            return Result.error(400, "分页参数不合法");
        }

        // 4. 分页查询全部文件
        Page<FileInfo> filePage =
                fileService.pageAll(page, size);

        // 5. 获取当前页所有文件的 userId
        List<Long> userIds = filePage.getRecords()
                .stream()
                .map(FileInfo::getUserId)
                .distinct()
                .toList();

        // 6. 批量查询用户信息
        List<SysUser> users = userIds.isEmpty()
                ? List.of()
                : sysUserMapper.selectBatchIds(userIds);

        // 7. 建立 userId 与 username 的映射
        Map<Long, String> usernameMap = new HashMap<>();

        for (SysUser user : users) {
            usernameMap.put(user.getId(), user.getUsername());
        }

        // 8. 将 FileInfo 转换为 AdminFileListItemDTO
        List<AdminFileListItemDTO> records =
                filePage.getRecords()
                        .stream()
                        .map(file -> new AdminFileListItemDTO(
                                file.getId(),
                                file.getUserId(),
                                usernameMap.getOrDefault(
                                        file.getUserId(), "未知用户"),
                                file.getOriginalName(),
                                file.getFileSize(),
                                file.getContentType(),
                                file.getCreateTime()
                        ))
                        .toList();

        // 9. 组织分页响应
        Map<String, Object> data = new HashMap<>();

        data.put("records", records);
        data.put("current", filePage.getCurrent());
        data.put("size", filePage.getSize());
        data.put("total", filePage.getTotal());
        data.put("pages", filePage.getPages());

        return Result.success("查询成功", data);
    }
}
