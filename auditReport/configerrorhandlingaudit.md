# 配置及错误处理安全审计记录

## 1. 基本信息

- 项目：spring-boot-file-security-lab
- 审计模块：应用配置、调试端点、异常处理
- 测试环境：本地 Spring Boot + MySQL + Postman
- 审计日期：2026/10/25
- 审计人员：ikun
- 代码版本或 Git Commit：37a3462

## 2. 审计目标

检查项目是否存在硬编码敏感配置、调试端点暴露、未授权管理接口，以及异常响应泄露内部实现细节等问题。

## 3. 测试记录

| 编号 | 测试内容                         | 预期结果                                               | 实际结果                                                     | 状态     |
| ---- | -------------------------------- | ------------------------------------------------------ | ------------------------------------------------------------ | -------- |
| C01  | 检查配置文件中的敏感信息         | 不存在已提交的真实敏感凭据                             | /src/main/resources/application.yml中datasource的账户密码明文显示<br /> | 已修复   |
| C02  | Git 敏感文件管理                 | 不存在                                                 | `.gitignore` 没有明确忽略 `.env` 文件或本地私密配置文件。这不代表仓库中一定存在这些文件，但未来开发时容易误提交。 | 已修复   |
| C03  | 检查配置文件中的硬编码信息       | 不存在                                                 | `file.upload-dir` 使用相对路径 `uploads` 本身不等于硬编码敏感信息，但生产环境最好通过配置明确上传目录，避免工作目录变化造成文件存储位置不确定 | 已修复   |
| C04  | 检查 Actuator 等管理端点         | 不暴露不必要的敏感信息或高权限操作                     | 检查 `pom.xml`，未发现 Spring Boot Actuator 依赖。测试 `GET /actuator`、`GET /test`、`GET /api/test`、`GET /debug` 和 `GET /api/debug`。相关请求均返回 404，未发现这些测试路径暴露管理信息或调试数据。其他端点以源码映射检查为准。 | 无异常   |
| E01  | 检查默认错误响应配置             | 未显式设置错误信息、堆栈和绑定错误的输出策略           | 发现/src/main/resources/application.yml当前配置没有明确设置以下错误响应选项 | 已修复   |
| E02  | 检查全局异常处理                 | 当前处理器主要处理文件上传异常，没有通用异常兜底处理器 | 1.提交格式错误的 JSON，返回400，Bad Request 的请求格式错误响应。响应中不出现 Java 堆栈、类名、源码路径、SQL 语句等内部信息。未因为请求格式错误导致应用崩溃。<br />2.验证未预期异常，编写一个临时的本地测试接口或自动化测试，主动抛出异常，创建AuditExceptionTestController测试类<br />@RestController<br/>@Profile("dev")<br/>@ConditionalOnProperty(<br/>        name = "audit.test-endpoints.enabled",<br/>        havingValue = "true"<br/>)<br/>@RequestMapping("/api/audit-test")<br/>public class AuditExceptionTestController {<br/>    @GetMapping("/exception")<br/>    public void triggerException() {<br/>        throw new IllegalStateException(<br/>                "E02-TEST-SECRET-SHOULD-NOT-LEAK"<br/>        );<br/>    }<br/>}<br />然后在配置中的程序实参中加入--spring.profiles.active=dev --audit.test-endpoints.enabled=true<br />启动测试，get http://localhost:8080/api/audit-test/exception，返回的 `500 Internal Server Error` 响应，检查 Response Body，未发现测试异常标记、Java 异常堆栈、源码路径及服务器绝对路径，符合预期。 | 暂无异常 |
| E03  | 检查文件接口异常响应             | 下载接口对部分路径及 I/O 异常返回通用错误              | 1.下载不存在的文件<br />先登录，取得有效 Token。然后请求一个确定不存在的文件 ID。返回404，文件不存在<br />2.数据库有记录，但磁盘文件不存在<br />先登录，取得有效 Token。随后上传一个有效文件，后将上传目录内的是有效文件移出该目录，随后调取http://localhost:8080/api/file/download/？（？为数据库的file_id），返回404，文件不存在，随后将文件放回原来的文件，正常可以访问<br /> | 无异常   |
| E04  | 检查无效 Token 响应              | TokenInterceptor 返回通用认证错误消息                  | 1.未添加 Authorization，未认证，返回 401<br/>2.token使用无效值，Bearer后附带过期 Token，返回认证失败，401<br/>3.Authorization中Bearer后不附带有效 Token	返回认证失败，401<br/>4.在Authorization标签中，Type 选择 `Basic Auth`，Username 填 `user`，Password 填 `pass`。返回认证失败，401<br/> | 暂无异常 |
| E05  | 检查异常响应中的 SQL、堆栈和路径 | 不向客户端泄露不必要的内部信息                         | 1.发现/src/main/resources/application.yml当前配置没有明确设置以下错误响应选项<br />2.sql注入测试，使用http://localhost:8080/api/user/login，<br />{ "username": "' OR '1'='1' -- ",   "password": "wrong-password" }，访问，返回401，用户名或者密码错误，未有异常 | 暂无异常 |



## 4. 修复记录

### 4.1、问题一

- 发现的问题：/src/main/resources/application.yml中datasource的账户密码明文显示。

- 风险等级：待评估

- 修复措施：在datasource中将明文改为获取环境变量，之前的账户密码最好进行修改

- ```java
  url: jdbc:mysql://localhost:3306/file_security_lab?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
  username: ${DB_USERNAME}
  password: ${DB_PASSWORD}
  增加启动时的环境变量，我数据库系统的真实username和password
  ```

- 修复代码位置：/src/main/resources/application.yml

- 修复后验证：无异常

### 4.2、问题二

- 发现的问题：`.gitignore` 没有明确忽略 `.env` 文件或本地私密配置文件。这不代表仓库中一定存在这些文件，但未来开发时容易误提交。

- 风险等级：待评估

- 修复措施：在/spring-boot-file-security-lab/\.gitignore增加

  ```java
  # Local secrets and environment files
  
  .env
  .env.*
  !.env.example
  
  # Local-only application configuration
  
  application-local.yml
  application-local.yaml
  application-local.properties
  ```

- 修复代码位置：/spring-boot-file-security-lab/\.gitignore

- 修复后验证：无异常

### 4.3、问题三

- `file.upload-dir` 使用相对路径 `uploads` ，被硬编码，最好通过配置明确上传目录，避免工作目录变化造成文件存储位置不确定。

- 风险等级：待评估

- 修复措施：增加src/main/java/com/example/filelab/config/FileStorageConfig.java，统一管理上传路径，到时下载、删除都使用，修改src/main/resources/application.yml             

- ```java
  1.src/main/java/com/example/filelab/config/FileStorageConfig.java
      @Value("${file.upload-dir}")
      private String uploadDir;
      private Path uploadRoot;
      @PostConstruct
      public void init() throws IOException {
          uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
          Files.createDirectories(uploadRoot);
          if (!Files.isDirectory(uploadRoot)|| !Files.isWritable(uploadRoot)) {
              throw new IllegalStateException("文件上传目录不可用" );
          }
      }
      public Path getUploadRoot() {
          return uploadRoot;
      }
  2.src/main/resources/application.yml配置upload-dir   
  file:
    upload-dir: ${FILE_UPLOAD_DIR:${user.home}/filelab-uploads}
    #优先读取环境变量 FILE_UPLOAD_DIR。
    #如果没有设置，就使用用户主目录下的 filelab-uploads 目录。
    #Windows 下通常会解析到类似 C:\Users\你的用户名\filelab-uploads 的位置。
    #Linux 下通常会解析到类似 /home/应用用户/filelab-uploads 的位置。
  ```

- 修复代码位置：src/main/java/com/example/filelab/config/FileStorageConfig.java，/src/main/resources/application.yml

- 修复后验证：无异常

### 4.4、问题四

- 发现/src/main/resources/application.yml当前配置没有明确设置以下错误响应选项。

- 风险等级：待评估

- 修复措施：/src/main/resources/application.yml         

- ```java
  server:
    error:
      include-stacktrace: never
      include-message: never
      include-binding-errors: never
  ```

- 修复代码位置：/src/main/resources/application.yml

- 修复后验证：无异常

## 5. 回归验证

| 编号 | 测试项目              | 实际结果 | 状态   |
| ---- | --------------------- | -------- | ------ |
| R01  | 正常登录和 Token 生成 | 待测试   | 待测试 |
| R02  | 获取当前用户信息      | 待测试   | 待测试 |
| R03  | 文件上传              | 待测试   | 待测试 |
| R04  | 文件下载              | 待测试   | 待测试 |
| R05  | 文件删除与所有权校验  | 待测试   | 待测试 |

## 6. 审计结论

待测试完成后填写。区分已确认的问题、未发现的问题和未覆盖的测试场景，不以“没有发现”代替“已证明不存在”。

## 7. Git 提交记录

- 提交哈希：37a3462
- 提交说明：待填写