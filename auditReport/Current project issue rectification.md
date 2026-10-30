# Current project issue rectification

### A01：上传失败时可能留下磁盘文件

- 代码文件和方法：FileController.java

- 复现条件：在FileController.java中

- ```java
  file.transferTo(savedPath);后加入运行异常抛出
  // 仅用于本地复现 模拟数据库保存失败
  throw new RuntimeException("A01-TEST-DB-INSERT-FAILED");*/
  后检查数据库，未插入数据，但是savepath下已保存文件。
  ```

- 可能影响：导致数据库数据和实际文件有出入。

- 修复建议：统一管理上传方法，将上传逻辑并入到service层。在fileservice中加入storeUploadedFile方法，统一管理。各自的异常各自管理。
  项目中每个类别做的事情

  `FileController`：接收 HTTP 请求、校验上传参数、返回接口响应。

  `FileService`：执行文件存储、数据库记录保存，以及失败后的文件清理。

  `FileInfoMapper`：负责数据库操作。

- 复测方法：

- ```java
  在保存时抛出该异常
  throw new IllegalStateException(
          "A01-TEST-DB-INSERT-FAILED"
  );
  后检查数据库，savepath下有无数据
  ```

- 状态：文件系统和数据库是两个独立的资源，Service 把逻辑放在一起，并不代表它们天然具有原子性。当前方案能处理应用捕获到的保存异常，但无法保证进程崩溃或断电时绝无孤儿文件；后续可以再增加定期清理任务。



### A02：注册接口的输入校验不完整

- 代码文件和方法。：SysUserController.java

- 复现条件：发现问题，

  1.用户名最大长度未与数据库 `username` 列保持一致。

  2.密码长度未设置合理上限。

  3.未明确密码最低长度要求。

  4.未使用 Bean Validation（例如 `@NotBlank`、`@Size`）统一校验请求 DTO。

- 可能影响：

- 修复建议：已在注册请求 DTO 中引入 Bean Validation（`@Valid`、`@Size`），统一校验用户名及密码的长度和空白输入；补充 BCrypt 输入字节长度限制，`@Valid`、`@Size`负责通用请求校验，字节检查负责 BCrypt 特有的限制；由全局异常处理器统一返回 HTTP 400；保留数据库用户名唯一约束作为并发场景下的数据完整性保障。

- 复测方法：

  | 编号       | 测试输入                                    | 结果     |
  | ---------- | ------------------------------------------- | -------- |
  | REG-VAL-01 | 用户名为空                                  | HTTP 400 |
  | REG-VAL-02 | 用户名全部为空格                            | HTTP 400 |
  | REG-VAL-03 | 用户名长度为 64 个字符                      | 成功注册 |
  | REG-VAL-04 | 用户名长度为 65 个字符                      | HTTP 400 |
  | REG-VAL-05 | 密码长度为 11 个字符                        | HTTP 400 |
  | REG-VAL-06 | 密码长度为 12 个字符，且不超过 72 字节      | 成功注册 |
  | REG-VAL-07 | 密码为 73 个 ASCII 字符                     | HTTP 400 |
  | REG-VAL-08 | 密码字符数不超过 72，但 UTF-8 字节数超过 72 | HTTP 400 |
  | REG-VAL-09 | 注册一个已经存在的用户名                    | HTTP 400 |

- 状态：已整改。



### A03：通用异常处理尚未覆盖

- 代码文件和方法：GlobalExceptionHandler.java

- 复现条件：打开一个不存在的地址/api/123，返回，notfound，没有进行统一管理；打开一个测试地址/api/audit-test/exception，返回 "status": 500, "error": "Internal Server Error" , "path": "/api/audit-test/exception"没有进行统一管理

- 可能影响：暴露不该暴露的系统信息，不便于管理异常情况。

- 修复建议：在GlobalExceptionHandler.java增加

  ```java
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<Result> handleNoResourceFound(
          NoResourceFoundException e) {
  return ResponseEntity
          .status(HttpStatus.NOT_FOUND)
          .body(Result.error(404, "请求的资源不存在"));
  }       
  @ExceptionHandler(Exception.class) //兜底异常返回
  public ResponseEntity<Result> handleGeneralException(Exception e) {
      log.error("发生未处理的服务端异常", e);
  
      return ResponseEntity
              .status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Result.error(500, "服务器内部错误，请稍后重试"));
  }
  ```

- 复测方法：

  | 测试请求                                                     | 结果                                   |
  | ------------------------------------------------------------ | -------------------------------------- |
  | 一个实际不存在的接口  GET /api/this-route-does-not-exist     | HTTP 404，"请求的资源不存在"           |
  | 一个实际存在、但会触发未处理异常的接口（/api/audit-test/exception） | HTTP 500，"服务器内部错误，请稍后重试" |
  | post http://localhost:8080/api/file/upload，文件超过大小     | HTTP 413 ，"文件大小超过限制"          |

- 状态：专用异常仍返回预期状态码；未处理异常返回 HTTP 500；响应中不包含 SQL、服务器路径、异常堆栈等内部细节。



### A04：Token 和登录限流是单机内存方案

- 代码文件和方法：TokenService.java/LoginAttemptService.java

- 可能影响：当前都使用 `ConcurrentHashMap` 保存状态。

- 限制：1.应用重启后，已签发的 Token 和登录失败计数都会丢失。

  ​           2.如果部署多个应用实例，每个实例的 Token 和失败计数互不共享。

- 状态：目前阶段先保留，后续再考虑使用共享存储管理会话和限流状态。



### A05：文件删除的磁盘与数据库一致性

- 代码文件和方法：FileController.java


- 可能影响：磁盘文件删除与数据库记录删除是两个独立操作，缺少失败补偿机制，数据库删除失败时可能出现数据库记录仍存在但磁盘文件已经丢失的情况。


- 修复建议：文件删除操作增加隔离目录和失败补偿机制。删除数据库记录前先将文件移动到隔离目录；数据库删除失败时尝试恢复原文件；数据库删除成功后清理隔离文件。同时增加路径校验、普通文件检查及并发删除测试。

  ```java
  //删除逻辑
  Path filePath;
  Path quarantinePath;
      try {
          // 获取上传目录和隔离目录
          Path uploadRoot = fileStorageConfig.getUploadRoot().toRealPath();
          Path quarantineRoot = fileStorageConfig.getQuarantineRoot().toRealPath();
          // 规范化数据库中的文件路径
          filePath = Paths.get(fileInfo.getFilePath()).toAbsolutePath().normalize();
          // 检查文件路径是否位于上传目录内
          if (filePath.equals(uploadRoot) || !filePath.startsWith(uploadRoot)) {    
              return Result.failure(400, "非法文件路径");
          }
          // 检查文件是否存在，不跟随符号链接
          if (!Files.exists(filePath, LinkOption.NOFOLLOW_LINKS)) {
              return Result.failure(404, "磁盘文件不存在");
          }
          // 拒绝符号链接及非普通文件
          if (Files.isSymbolicLink(filePath) || !Files.isRegularFile(filePath, LinkOption.NOFOLLOW_LINKS)) {
              return Result.failure(400, "目标文件类型不合法");
          }
          // 再次确认文件真实路径位于上传目录内
          Path realFilePath = filePath.toRealPath();
          if (!realFilePath.startsWith(uploadRoot)) {
              return Result.failure(400, "非法文件路径");
          }
          // 检查隔离目录不能位于上传目录内部
          if (quarantineRoot.startsWith(uploadRoot)) {    
              return Result.failure(500, "文件存储配置错误");
          }
          // 生成随机隔离文件名
          quarantinePath = quarantineRoot.resolve(UUID.randomUUID() + ".tmp");
          // 先将原文件移动到隔离目录，核心操作Files.move
          try {
              Files.move(filePath, quarantinePath, StandardCopyOption.ATOMIC_MOVE);
          } catch (AtomicMoveNotSupportedException e) {
              return Result.failure(500, "文件暂存失败");
          }
          // 删除数据库记录
          int rows;
          try {
              rows = fileService.deleteOwnedFile(id, userId);
          } catch (RuntimeException dbException) {
              // 数据库操作异常，尝试恢复原文件
              restoreFile(quarantinePath, filePath);
              return Result.failure(500, "数据库删除失败");
          }
          // 检查数据库删除结果
          if (rows != 1) {
              // 数据库没有成功删除记录，恢复原文件
              boolean restored = restoreFile(quarantinePath, filePath);
              if (!restored) {
                  return Result.failure(500, "删除失败，文件已暂存，需人工恢复");
              }
              return Result.failure(500, "数据库记录删除失败");
          }
          // 数据库删除成功，清理隔离文件
          try {
              Files.deleteIfExists(quarantinePath);
          } catch (IOException cleanupException) {    
          }
          return Result.success("文件删除成功");
      } catch (InvalidPathException e) {
          return Result.failure(400, "文件路径不合法");
      } catch (IOException e) {
          return Result.failure(500, "文件删除失败");
      }
  }
  //将隔离目录中的文件恢复到原始位置。
  private boolean restoreFile(Path quarantinePath, Path originalPath) {
      try {
          // 隔离文件不存在，无法恢复
          if (!Files.exists(quarantinePath, LinkOption.NOFOLLOW_LINKS)) {   
              return false;
          }
          // 原始位置已经存在文件时，不覆盖
          if (Files.exists(originalPath, LinkOption.NOFOLLOW_LINKS)) {
              return false;
          }
          // 使用原子移动恢复文件
          Files.move(quarantinePath, originalPath, StandardCopyOption.ATOMIC_MOVE);
          return true;
      } catch (IOException e) {
          return false;
      }
  }
  ```

- 复测方法：

- | 编号   | 测试步骤                                                     | 结果                                                         |
  | ------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
  | DEL-01 | 用户 A 上传文件，再删除自己的文件                            | 成功，临时文件区有文件，后面数据库，临时区，原文件区均删除   |
  | DEL-02 | 用户 A 尝试删除用户 B 的文件                                 | 返回 404，文件和数据库记录均保留                             |
  | DEL-03 | 删除不存在的文件 ID                                          | 返回 404，文件不存在                                         |
  | DEL-04 | 手动删除磁盘文件，再调用删除接口                             | 返回404，数据库存在文件记录，但磁盘文件不存在，fileId=51, path=C:\Users\***\filelab-uploads\*.pdf |
  | DEL-05 | 模拟数据库删除失败，<br />步骤：1.开发环境临时修改 FileService.deleteOwnedFile()，让它返回 0；<br />2.调用删除接口。 | 返回500，数据库删除失败，文件恢复成功，原始路径：C:\Users\***\filelab-uploads\*.pdf |
  | DEL-06 | 修改数据库文件路径，使其指向上传目录外                       | 返回400，拒绝删除非法路径文件，fileId=51, userId=**, path=C:\Users\8888\Desktop\素材库\图片1.png |
  | DEL-07 | 模拟隔离文件清理失败<br />步骤：1.在删除临时文件中Files.deleteIfExists(quarantinePath)后直接抛出异常来主动模拟隔离文件清理失败，throw new IOException("DEL-07-TEST-CLEANUP-FAILED");  <br />2.后调用删除接口。 | 数据库记录已删除，但隔离文件清理失败，fileId=51, quarantinePath=C:\Users\2222\filelab-quarantine\666.tmp |
  | DEL-08 | 两个请求同时删除同一个文件<br />步骤：使用Apache JMeter测试并发，第一步：创建测试计划，打开 JMeter 后：「测试计划」「线程组」，在右侧设置：线程数（用户数）2，Ramp-Up 时间（秒）1，循环次数1<br />第二步：添加同步定时器，为了尽量让两个请求同时发出：「线程组」「添加」→「定时器」→「同步定时器」，模拟用户组的数量2，超时时间（毫秒）5000<br />第三步：添加 HTTP 请求，「线程组」「添加」→「取样器」→「HTTP 请求」，名称：DEL-08-并发删除，协议：http，服务器名称或 IP：localhost，端口号：8080，方法：DELETE，路径：/api/file/文件ID<br />配置 Authorization 请求头，「线程组」「添加」→「配置元件」→「HTTP 信息头管理器」。添加一行：Authorization `Bearer 有效Token`<br />添加结果查看器，「线程组」，选择「添加」→「监听器」→「察看结果树」。再添加一个「汇总报告」，方便查看两个请求的结果。<br />执行测试，点击 JMeter 上方绿色三角形「启动」。 | 两个请求同时删除同一个文件，只有一个请求返回成功；另一个请求返回明确的失败结果；最终数据库记录和原始文件均不存在 |

- 状态：文件删除操作增加隔离目录和失败补偿机制。删除数据库记录前先将文件移动到隔离目录；数据库删除失败时尝试恢复原文件；数据库删除成功后清理隔离文件。同时增加路径校验、普通文件检查及并发删除测试。




### A06：注册接口存在并发竞态

当前流程是：

```
SysUser existingUser = sysUserMapper.selectOne(
    Wrappers.<SysUser>lambdaQuery()
        .eq(SysUser::getUsername, username)
);

if (existingUser != null) {
    return Result.error(409, "用户名已存在");
}

sysUserMapper.insert(user);
```

如果两个并发请求使用同一个用户名，可能同时通过前面的存在性检查，然后同时尝试插入。

因此，数据库必须有 `username` 的唯一约束；Java 层的查询只能用于改善正常情况下的提示，不能替代数据库约束。

建议捕获并识别重复键异常，将其转换成稳定的 HTTP `409 Conflict` 或项目约定的业务响应，避免并发情况下直接返回未处理的服务器错误。





代码文件和方法。

复现条件。

可能影响。

修复建议。

复测方法。

状态：已确认、待整改、待复测或设计限制。



层级	已检查的内容	结论
Controller	SysUserController、FileController	已发现输入校验、异常补偿和并发处理方面的改进点
Service	FileService、TokenService、LoginAttemptService	文件所有权查询和 Token 过期逻辑已有基础；内存存储有单机限制
Mapper	SysUserMapper、FileInfoMapper	使用 MyBatis-Plus BaseMapper，当前已查看代码未发现手写 SQL 拼接
配置与拦截器	FileStorageConfig、TokenInterceptor、WebConfig	Token 校验和请求拦截已有实现；上传目录配置需要修正