package com.example.filelab.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.filelab.common.FileUploadException;
import com.example.filelab.common.Result;
import com.example.filelab.config.FileStorageConfig;
import com.example.filelab.dto.FileListItemDTO;
import com.example.filelab.entity.FileInfo;
import com.example.filelab.mapper.FileInfoMapper;
import com.example.filelab.service.FileService;
import com.example.filelab.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;


@RestController
@RequestMapping("/api/file")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".png", ".jpg", ".jpeg");
    private final FileInfoMapper fileInfoMapper;
    private final FileService fileService;
    //@Value("${file.upload-dir:uploads}")
    //@Value("${file.upload-dir}")
    private final TokenService tokenService;
    private final FileStorageConfig fileStorageConfig;

    public FileController(FileInfoMapper fileInfoMapper, FileService fileService, TokenService tokenService, FileStorageConfig fileStorageConfig) {
        this.fileInfoMapper = fileInfoMapper;
        this.fileService = fileService;
        this.tokenService = tokenService;
        this.fileStorageConfig = fileStorageConfig;
    }

    //基础签名验证
    private String detectMimeType(MultipartFile file) throws IOException {

        byte[] header;

        try (InputStream input = file.getInputStream()) {
            header = input.readNBytes(8);
        }

        // PNG 文件签名
        byte[] pngSignature = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

        if (startsWith(header, pngSignature)) {
            return "image/png";
        }

        // JPEG 文件签名的前三个字节
        if (header.length >= 3 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }

        // PDF 文件头
        byte[] pdfSignature = "%PDF-".getBytes(StandardCharsets.US_ASCII);

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
        if (name.isEmpty() || name.equals(".") || name.equals("..") || name.length() > 255 || name.contains(":") || name.chars().anyMatch(Character::isISOControl)) {
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

    private boolean extensionMatchesMime(String extension, String detectedMime) {

        if (extension == null || detectedMime == null) {
            return false;
        }

        return switch (extension.toLowerCase(Locale.ROOT)) {
            case ".pdf" -> "application/pdf".equals(detectedMime);

            case ".png" -> "image/png".equals(detectedMime);

            case ".jpg", ".jpeg" -> "image/jpeg".equals(detectedMime);

            default -> false;
        };
    }

    /*@GetMapping("/list")
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
    }*/

    @GetMapping("/list")
    public Result<Map<String, Object>> list(@RequestAttribute("userId") Long userId, @RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "10") long size) {

        // 校验分页参数
        if (page < 1) {
            return Result.error(400, "页码必须大于等于1");
        }

        if (size < 1 || size > 100) {
            return Result.error(400, "每页数量必须在1到100之间");
        }

        // 根据当前登录用户 ID 查询，不接受客户端传入 userId
        Page<FileInfo> result = fileService.listByUserId(userId, page, size);

        // 将数据库实体转换为对外 DTO
        List<FileListItemDTO> records = result.getRecords().stream().map(file -> new FileListItemDTO(file.getId(), file.getOriginalName(), file.getFileSize(), file.getContentType(), file.getCreateTime())).toList();

        // 分页响应数据
        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        data.put("total", result.getTotal());
        data.put("pages", result.getPages());

        return Result.success("查询成功", data);
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

        //Path savedPath = null;

        try {
            // 创建上传目录
            //Path directory = fileStorageConfig.getUploadRoot();

            //Files.createDirectories(directory);

            // 使用随机文件名，避免用户通过原始文件名控制保存路径，只处理了空文件名，没有对文件名长度、路径信息、控制字符等进行校验。
           /* String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isBlank()) {
                originalName = "unknown";
            }*/

            String originalName = sanitizeOriginalName(file.getOriginalFilename());

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
            String extension = originalName.substring(dotIndex).toLowerCase(Locale.ROOT);

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
            if (!extensionMatchesMime(extension, detectedMime)) {

                return Result.error(400, "文件扩展名与实际内容不匹配");
            }

            /*String storedName = UUID.randomUUID() + extension;
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

            // 仅用于本地复现 模拟数据库保存失败
            throw new RuntimeException("A02-TEST-DB-INSERT-FAILED");*/

            //int rows = fileInfoMapper.insert(info);

           /* if (rows != 1) {
                Files.deleteIfExists(savedPath);
                return Result.error(500, "文件信息保存失败");
            }

            return Result.success("上传成功", Map.of("id", info.getId(), "originalName", info.getOriginalName(), "storedName", info.getStoredName(), "fileSize", info.getFileSize()));
*/

            FileInfo info = fileService.storeUploadedFile(file, userId, originalName, extension, detectedMime);

            return Result.success("上传成功", Map.of("id", info.getId(), "originalName", info.getOriginalName(), "storedName", info.getStoredName(), "fileSize", info.getFileSize()));

        /*} catch (IOException e) {
            // 如果保存过程中发生 IO 错误，尝试清理已创建的文件
            if (savedPath != null) {
                try {
                    Files.deleteIfExists(savedPath);
                } catch (IOException ignored) {
                    // 保留原始异常，不覆盖它
                }
            }
            return Result.error(500, "文件保存失败");
        }*/

        } catch (FileUploadException e) {
            // 对客户端返回通用错误，不暴露数据库异常或本地路径
            return Result.error(500, "文件保存失败");

        } catch (IOException e) {
            // 例如读取上传文件签名时发生 IO 异常
            return Result.error(500, "文件处理失败");
        }

    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<?> downloadFile(@PathVariable("fileId") Long fileId, @RequestHeader(value = "Authorization", required = false) String authorization) {

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

        /*// 5. 验证文件物理路径并读取文件
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
        }*/

        try {
            // 4. 获取已经解析为真实路径的上传目录
            Path rootPath = fileStorageConfig.getUploadRoot();

            // 5. 获取数据库中的文件路径
            Path storedPath = Paths.get(fileInfo.getFilePath()).toAbsolutePath().normalize();

            // 6. 明确拒绝文件本身是符号链接的情况
            if (Files.isSymbolicLink(storedPath)) {
                log.warn("拒绝下载符号链接文件，fileId={}, userId={}", fileId, userId);

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Result.failure(404, "文件不存在"));
            }

            // 7. 解析文件真实路径
            Path realFilePath = storedPath.toRealPath();

            // 8. 检查真实路径必须位于上传目录内
            if (realFilePath.equals(rootPath) || !realFilePath.startsWith(rootPath)) {

                log.warn("拒绝下载上传目录之外的文件，fileId={}, userId={}, path={}", fileId, userId, realFilePath);

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Result.failure(404, "文件不存在"));
            }

            // 9. 检查是否为普通文件，不跟随最终路径上的符号链接
            if (Files.isSymbolicLink(realFilePath) || !Files.isRegularFile(realFilePath, LinkOption.NOFOLLOW_LINKS) || !Files.isReadable(realFilePath)) {

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Result.failure(404, "文件不存在"));
            }

            // 10. 准备下载文件名
            String downloadName = fileInfo.getOriginalName();

            if (downloadName == null || downloadName.isBlank()) {
                downloadName = "download";
            }

            String disposition = ContentDisposition.attachment().filename(downloadName, StandardCharsets.UTF_8).build().toString();

            // 11. 通过 NOFOLLOW_LINKS 打开文件，拒绝打开最终路径上的符号链接
            FileChannel channel = FileChannel.open(realFilePath, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);

            long contentLength;

            try {
                // 从已经打开的文件句柄获取文件大小
                contentLength = channel.size();

            } catch (IOException e) {
                channel.close();
                throw e;
            }

            // 12. 使用已经打开的文件句柄构建资源
            InputStreamResource resource = new InputStreamResource(Channels.newInputStream(channel));

            // 13. 返回文件
            // 响应发送使用已经打开的文件句柄，不再重新按路径打开
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).header(HttpHeaders.CONTENT_DISPOSITION, disposition).contentLength(contentLength).body(resource);

        } catch (IOException | InvalidPathException e) {

            log.warn("文件下载失败，fileId={}, userId={}", fileId, userId, e);

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Result.failure(404, "文件不存在"));
        }
    }

    @DeleteMapping("/{id}")
    public Result deleteFile(@PathVariable("id") Long id, HttpServletRequest request) {

        // 1. 获取当前登录用户 ID
        Long userId = (Long) request.getAttribute("userId");

        if (userId == null) {
            return Result.failure(401, "用户未登录");
        }

        // 2. 校验参数
        if (id == null || id <= 0) {
            return Result.failure(400, "文件 ID 不合法");
        }

        // 3. 查询文件，同时校验所有权
        FileInfo fileInfo = fileService.getOwnedFile(id, userId);

        if (fileInfo == null) {
            // 不区分文件不存在和不属于当前用户
            return Result.failure(404, "文件不存在");
        }

        Path filePath;
        Path quarantinePath;
        try {
            // 4. 获取 FileStorageConfig 已经解析过的真实路径
            Path uploadRoot = fileStorageConfig.getUploadRoot();
            Path quarantineRoot = fileStorageConfig.getQuarantineRoot();

            // 5. 检查数据库中的文件路径
            Path storedPath = Paths.get(fileInfo.getFilePath()).toAbsolutePath().normalize();

            // 6. 检查文件是否存在，不跟随符号链接
            if (!Files.exists(storedPath, LinkOption.NOFOLLOW_LINKS)) {

                log.error("数据库存在文件记录，但磁盘文件不存在，fileId={}, path={}", id, storedPath);

                return Result.failure(404, "磁盘文件不存在");
            }

            // 7. 明确拒绝文件本身是符号链接
            if (Files.isSymbolicLink(storedPath)) {

                log.warn("拒绝删除符号链接文件，fileId={}, userId={}, path={}", id, userId, storedPath);

                return Result.failure(400, "不允许删除符号链接");
            }

            // 8. 拒绝目录、设备文件等非普通文件
            if (!Files.isRegularFile(storedPath, LinkOption.NOFOLLOW_LINKS)) {

                log.warn("拒绝删除非普通文件，fileId={}, path={}", id, storedPath);

                return Result.failure(400, "目标文件类型不合法");
            }

            // 9. 解析文件真实路径
            Path realFilePath = storedPath.toRealPath();

            // 10. 使用真实路径检查文件是否属于上传目录
            if (realFilePath.equals(uploadRoot) || !realFilePath.startsWith(uploadRoot)) {

                log.warn("拒绝删除上传目录之外的文件，fileId={}, userId={}, path={}", id, userId, realFilePath);

                return Result.failure(400, "非法文件路径");
            }

            // 11. 确认隔离目录不在上传目录内部
            if (quarantineRoot.startsWith(uploadRoot)) {

                log.error("隔离目录不能位于上传目录内部");

                return Result.failure(500, "文件存储配置错误");
            }

            // 12. 后续移动和恢复都使用真实文件路径
            filePath = realFilePath;

            quarantinePath = quarantineRoot.resolve(UUID.randomUUID() + ".tmp");

            // 先移动到隔离目录
            try {
                Files.move(filePath, quarantinePath, StandardCopyOption.ATOMIC_MOVE);

            } catch (AtomicMoveNotSupportedException e) {

                log.error("当前文件系统不支持原子移动，fileId={}", id, e);

                return Result.failure(500, "文件暂存失败");
            }
            // 13. 删除数据库记录
            int rows;

            try {
                rows = fileService.deleteOwnedFile(id, userId);

            } catch (RuntimeException dbException) {

                boolean restored = restoreFile(quarantinePath, filePath);

                log.error("删除文件数据库记录时发生异常，fileId={}, userId={}, restored={}", id, userId, restored, dbException);

                if (!restored) {
                    return Result.failure(500, "数据库删除失败，文件恢复失败，需人工处理");
                }

                return Result.failure(500, "数据库删除失败，文件已恢复");
            }

            // 14. 检查数据库删除结果
            if (rows != 1) {

                // 数据库没有成功删除记录，恢复原文件
                boolean restored = restoreFile(quarantinePath, filePath);

                if (!restored) {
                    log.error("数据库删除失败且文件恢复失败，fileId={}, quarantinePath={}", id, quarantinePath);

                    return Result.failure(500, "删除失败，文件已暂存，需人工恢复");
                }

                return Result.failure(500, "数据库记录删除失败");
            }

            // 15. 数据库删除成功，清理隔离文件
            try {
                // DEL-07 测试：主动模拟隔离文件清理失败
                //throw new IOException("DEL-07-TEST-CLEANUP-FAILED");
                Files.deleteIfExists(quarantinePath);

            } catch (IOException cleanupException) {

                // 数据库已经删除成功，不能再返回数据库删除失败
                log.error("数据库记录已删除，但隔离文件清理失败，fileId={}, quarantinePath={}", id, quarantinePath, cleanupException);
            }

            return Result.success("文件删除成功");

        } catch (InvalidPathException e) {

            log.error("数据库中的文件路径格式非法，fileId={}", id, e);

            return Result.failure(400, "文件路径不合法");

        } catch (IOException e) {

            log.error("文件删除过程中发生磁盘操作异常，fileId={}, userId={}", id, userId, e);

            return Result.failure(500, "文件删除失败");
        }
    }

    /**
     * 将隔离目录中的文件恢复到原始位置。
     *
     * @param quarantinePath 隔离文件路径
     * @param originalPath   原始文件路径
     * @return 是否恢复成功
     */
    private boolean restoreFile(Path quarantinePath, Path originalPath) {

        try {

            // 隔离文件不存在，无法恢复
            if (!Files.exists(quarantinePath, LinkOption.NOFOLLOW_LINKS)) {

                log.error("恢复失败，隔离文件不存在：{}", quarantinePath);

                return false;
            }

            // 原始位置已经存在文件时，不覆盖
            if (Files.exists(originalPath, LinkOption.NOFOLLOW_LINKS)) {

                log.error("恢复失败，原始文件位置已存在其他文件：{}", originalPath);

                return false;
            }

            // 使用原子移动恢复文件
            Files.move(quarantinePath, originalPath, StandardCopyOption.ATOMIC_MOVE);

            log.info("文件恢复成功，原始路径：{}", originalPath);

            return true;

        } catch (IOException e) {

            log.error("文件恢复失败，隔离路径：{}，原始路径：{}", quarantinePath, originalPath, e);

            return false;
        }
    }
}