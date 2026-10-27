package com.example.filelab.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.filelab.common.Result;
import com.example.filelab.config.FileStorageConfig;
import com.example.filelab.dto.FileListItemDTO;
import com.example.filelab.entity.FileInfo;
import com.example.filelab.mapper.FileInfoMapper;
import com.example.filelab.service.FileService;
import com.example.filelab.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;


@RestController
@RequestMapping("/api/file")
public class FileController {

    private final FileInfoMapper fileInfoMapper;
    private final FileService fileService;
    private final TokenService tokenService;
    private final FileStorageConfig fileStorageConfig;
    //@Value("${file.upload-dir:uploads}")
   // @Value("${file.upload-dir}")

    private static final long MAX_FILE_SIZE =
            10L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of(".pdf", ".png", ".jpg", ".jpeg");
    public FileController(FileInfoMapper fileInfoMapper, FileService fileService, TokenService tokenService, FileStorageConfig fileStorageConfig) {
        this.fileInfoMapper = fileInfoMapper;
        this.fileService = fileService;
        this.tokenService = tokenService;
        this.fileStorageConfig = fileStorageConfig;
    }
    //基础签名验证
    private String detectMimeType(MultipartFile file)
            throws IOException {

        byte[] header;

        try (InputStream input = file.getInputStream()) {
            header = input.readNBytes(8);
        }

        // PNG 文件签名
        byte[] pngSignature = {
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A
        };

        if (startsWith(header, pngSignature)) {
            return "image/png";
        }

        // JPEG 文件签名的前三个字节
        if (header.length >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }

        // PDF 文件头
        byte[] pdfSignature =
                "%PDF-".getBytes(StandardCharsets.US_ASCII);

        if (startsWith(header, pdfSignature)) {
            return "application/pdf";
        }

        return null;
    }

    //增加文件名校验方法
     String sanitizeOriginalName(String rawName) {

        if (rawName == null || rawName.isBlank()) {
            return null;
        }

        // 统一路径分隔符，再只保留文件名部分
        String name = rawName.replace('\\', '/');

        name = name.substring(name.lastIndexOf('/') + 1).trim();

        // 文件名长度、特殊名称和控制字符检查
        if (name.isEmpty()
                || name.equals(".")
                || name.equals("..")
                || name.length() > 255
                || name.contains(":")
                || name.chars().anyMatch(Character::isISOControl)) {
            return null;
        }

        return name;
    }
    private boolean startsWith(byte[] data, byte[] prefix) {

        if (data.length < prefix.length) {
            return false;
        }

        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }

        return true;
    }

    private boolean extensionMatchesMime(
            String extension,
            String detectedMime) {

        if (extension == null || detectedMime == null) {
            return false;
        }

        return switch (extension.toLowerCase(Locale.ROOT)) {
            case ".pdf" ->
                    "application/pdf".equals(detectedMime);

            case ".png" ->
                    "image/png".equals(detectedMime);

            case ".jpg", ".jpeg" ->
                    "image/jpeg".equals(detectedMime);

            default -> false;
        };
    }

    @GetMapping("/list")
    public ResponseEntity<Result<List<FileListItemDTO>>> listFiles(@RequestHeader(value = "Authorization", required = false) String authorization) {

        // 1. 检查 Authorization 请求头
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.<List<FileListItemDTO>>failure(401, "缺少或无效的Token"));
        }

        // 2. 提取 Token
        String token = authorization.substring(7).trim();

        if (token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.<List<FileListItemDTO>>failure(401, "缺少或无效的Token"));
        }

        // 3. 通过服务器端 Token 存储识别当前用户
        Long userId = tokenService.getUserId(token);

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.<List<FileListItemDTO>>failure(401, "缺少或无效的Token"));
        }

        // 4. 只查询当前用户的文件
        List<FileListItemDTO> files = fileService.listByUserId(userId);

        // 5. 返回结果
        return ResponseEntity.ok(Result.success(files));
    }

    @PostMapping("/upload")
    public Result<?> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {

        if (file == null || file.isEmpty()) {
            return Result.error(400, "请选择有效的文件");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return Result.error(413, "单个文件不能超过 10 MiB");
        }


        // TokenInterceptor 验证通过后，应该把 userId 放入 request
        Long userId = (Long) request.getAttribute("userId");

        if (userId == null) {
            return Result.error(401, "未登录或 Token 无效");
        }

        Path savedPath = null;

        try {
            // 创建上传目录
            Path directory = fileStorageConfig.getUploadRoot();

            Files.createDirectories(directory);

            // 使用随机文件名，避免用户通过原始文件名控制保存路径，只处理了空文件名，没有对文件名长度、路径信息、控制字符等进行校验。
           /* String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isBlank()) {
                originalName = "unknown";
            }*/

            String originalName =
                    sanitizeOriginalName(file.getOriginalFilename());

            if (originalName == null) {
                return Result.error(400, "文件名非法或长度超出限制");
            }

          /*String extension;
            int dotIndex = originalName.lastIndexOf('.');
            if (dotIndex >= 0) {
                String candidate = originalName.substring(dotIndex);
                if (candidate.matches("\\.[a-zA-Z0-9]{1,10}")) {
                    extension=candidate;
                }
            }*/
            //之前只检验了文件匹配名字，后续改只能传".pdf", ".png", ".jpg", ".jpeg"的文件*/

            // 检查文件名是否包含有效扩展名
            int dotIndex = originalName.lastIndexOf('.');

            if (dotIndex <= 0 || dotIndex == originalName.length() - 1) {
                return Result.error(400, "文件名或扩展名无效");
            }

           // 提取扩展名并统一转换为小写
            String extension = originalName.substring(dotIndex)
                    .toLowerCase(Locale.ROOT);

            // 只允许白名单中的文件扩展名
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                return Result.error(400, "不支持的文件类型");
            }

            // 检测文件实际内容类型，而不是相信客户端声明的 Content-Type
            String detectedMime = detectMimeType(file);

            if (detectedMime == null) {
                return Result.error(400, "无法识别或不支持的文件内容");
            }

            // 无法识别文件类型，或者扩展名与实际类型不匹配时，拒绝上传
            if (detectedMime == null
                    || !extensionMatchesMime(extension, detectedMime)) {

                return Result.error(
                        400,
                        "文件扩展名与实际内容不匹配"
                );
            }

            String storedName = UUID.randomUUID() + extension;
            savedPath = directory.resolve(storedName).normalize();

            // 先保存实际文件到磁盘
            file.transferTo(savedPath);

            // 再保存文件信息到数据库
            FileInfo info = new FileInfo();
            info.setUserId(userId);
            info.setOriginalName(originalName);
            info.setStoredName(storedName);
            info.setFilePath(savedPath.toString());
            info.setFileSize(file.getSize());
            //info.setContentType(file.getContentType());
            info.setContentType(detectedMime);
            info.setCreateTime(LocalDateTime.now());

            int rows = fileInfoMapper.insert(info);

            if (rows != 1) {
                Files.deleteIfExists(savedPath);
                return Result.error(500, "文件信息保存失败");
            }

            return Result.success("上传成功", Map.of("id", info.getId(), "originalName", info.getOriginalName(), "storedName", info.getStoredName(), "fileSize", info.getFileSize()));

        } catch (IOException e) {
            // 如果保存过程中发生 IO 错误，尝试清理已创建的文件
            if (savedPath != null) {
                try {
                    Files.deleteIfExists(savedPath);
                } catch (IOException ignored) {
                    // 保留原始异常，不覆盖它
                }
            }
            return Result.error(500, "文件保存失败");
        }
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<?> downloadFile(@PathVariable("fileId")  Long fileId, @RequestHeader(value = "Authorization", required = false) String authorization) {

        // 1. 检查 Authorization 请求头
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.failure(401, "缺少或无效的Token"));
        }

        // 2. 提取 Token
        String token = authorization.substring(7).trim();

        if (token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.failure(401, "缺少或无效的Token"));
        }

        // 3. 根据 Token 获取当前用户 ID
        Long userId = tokenService.getUserId(token);

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.failure(401, "缺少或无效的Token"));
        }

        // 4. 查询当前用户拥有的文件
        FileInfo fileInfo = fileService.getOwnedFile(fileId, userId);

        if (fileInfo == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Result.failure(404, "文件不存在"));
        }

        // 5. 验证文件物理路径并读取文件
        try {
            Path rootPath = fileStorageConfig.getUploadRoot();

            Path realFilePath = Paths.get(fileInfo.getFilePath()).toRealPath();

            // 确保文件在配置的上传目录内
            if (!realFilePath.startsWith(rootPath) || !Files.isRegularFile(realFilePath) || !Files.isReadable(realFilePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Result.failure(404, "文件不存在"));
            }

            // 6. 设置下载文件名
            String downloadName = fileInfo.getOriginalName();

            if (downloadName == null || downloadName.isBlank()) {
                downloadName = "download";
            }

            FileSystemResource resource = new FileSystemResource(realFilePath);

            String disposition = ContentDisposition.attachment().filename(downloadName, StandardCharsets.UTF_8).build().toString();

            // 7. 返回文件内容
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).header(HttpHeaders.CONTENT_DISPOSITION, disposition).contentLength(Files.size(realFilePath)).body(resource);

        } catch (IOException | InvalidPathException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Result.failure(404, "文件不存在"));
        }
    }

    @DeleteMapping("/{id}")
    public Result deleteFile(@PathVariable("id")  Long id, HttpServletRequest request) {

        // 1. 从拦截器中获取当前登录用户 ID
        Long userId = (Long) request.getAttribute("userId");

        if (userId == null) {
            return Result.failure(401,"用户未登录");
        }

        // 2. 查询文件，并同时校验文件所有权
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(FileInfo::getId, id).eq(FileInfo::getUserId, userId);

        FileInfo fileInfo = fileInfoMapper.selectOne(wrapper);

        // 文件不存在或不属于当前用户，统一返回
        if (fileInfo == null) {
            return Result.failure(404, "文件不存在");
        }

        try {
            // 3. 校验文件路径，确保文件位于上传目录中
            Path uploadRoot = fileStorageConfig.getUploadRoot();

            Path filePath = Paths.get(fileInfo.getFilePath()).toAbsolutePath().normalize();

            if (filePath.equals(uploadRoot) || !filePath.startsWith(uploadRoot)) {
                return Result.failure(400,"非法文件路径");
            }

            // 4. 删除磁盘文件
            Files.deleteIfExists(filePath);

            LambdaQueryWrapper<FileInfo> deleteWrapper =
                    new LambdaQueryWrapper<>();

            deleteWrapper.eq(FileInfo::getId, id)
                    .eq(FileInfo::getUserId, userId);

            // 5. 删除数据库记录
            int rows = fileService.deleteOwnedFile(id, userId);

            if (rows != 1) {
                return Result.failure(500,"数据库记录删除失败");
            }

            return Result.success("文件删除成功");

        } catch (IOException e) {
            return Result.failure(500,"文件删除失败");
        }
    }

}