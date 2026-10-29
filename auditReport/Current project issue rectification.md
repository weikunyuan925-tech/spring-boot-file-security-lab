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

文件：

- [TokenService.java](https://github.com/weikunyuan925-tech/spring-boot-file-security-lab/blob/main/src/main/java/com/example/filelab/service/TokenService.java?utm_source=chatgpt.com)
- [LoginAttemptService.java](https://github.com/weikunyuan925-tech/spring-boot-file-security-lab/blob/main/src/main/java/com/example/filelab/service/LoginAttemptService.java?utm_source=chatgpt.com)

当前两者都使用 `ConcurrentHashMap` 保存状态。

这对你的本地学习项目是合理的简化方案，但有两个限制：

1. 应用重启后，已签发的 Token 和登录失败计数都会丢失。
2. 如果部署多个应用实例，每个实例的 Token 和失败计数互不共享。

第二点尤其需要注意：多实例部署时，同一个 Token 在一个实例中签发，不一定能在另一个实例中验证成功；攻击者也可能利用不同实例分别尝试登录。

建议把这项记录为架构限制，而不是当前已确认的漏洞。单机学习阶段可以先保留，后续再考虑使用共享存储管理会话和限流状态。

### A06：文件删除的磁盘与数据库一致性

文件：[FileController.java](https://github.com/weikunyuan925-tech/spring-boot-file-security-lab/blob/main/src/main/java/com/example/filelab/controller/FileController.java?utm_source=chatgpt.com)

当前删除流程先删除磁盘文件，再删除数据库记录。

如果磁盘删除成功，但数据库删除失败，就可能出现数据库中仍保留记录、实际文件却已不存在的情况。

建议明确失败处理策略，并增加测试验证：

- 正常删除：磁盘文件和数据库记录都被删除。
- 数据库删除失败：系统能否识别并记录不一致状态。
- 磁盘文件已不存在：接口能否安全处理并按约定清理数据库记录。
- 删除其他用户文件：仍然返回受控的不存在或无权访问响应。

你目前的所有权查询条件包含文件 ID 和用户 ID，这是正确的方向，不要为了处理一致性问题而去掉它。

### A07：注册接口存在并发竞态

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