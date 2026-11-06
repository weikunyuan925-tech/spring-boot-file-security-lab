【Day 1 打卡】

日期：2026/10/9

一、今日主线任务
- [x] 准备环境（JDK、IDEA、Maven、MySQL、Git、Postman）
- [x] 创建本地学习目录 (studyNotes)
- [x] 编写学习计划 (oneMonthPlan.md)
- [x] 创建GitHub仓库 (spring-boot-file-security-lab)
- [x] 初始化Git并完成第一次提交推送

二、今日产出
- GitHub 仓库地址：https://github.com/weikunyuan925-tech/spring-boot-file-security-lab

- 踩坑与解决记录：
  1. 遇到 Author identity unknown，用 git config 配置了用户名和邮箱解决。
  
     git config --global user.name "ikun"
     git config --global user.email "weikunyuan925@gmail.com"
  
  2. 遇到 push rejected，因为远程仓库有旧文件，使用强制推送解决。
  
     git push -u origin main --force

  3.每次更新都要

​      git add .
​      git commit -m "写清楚你改了什么"
​      git push



【Day 2 打卡】

日期：2026/10/10

一、今日主线任务

【Day 2 打卡】
- [x] 成功创建 Spring Boot 项目
- [x] 项目能在 IDEA 中启动
- [x] 浏览器成功访问 /hello
- [x] 已完成 Git commit
- [x] 代码已推送至 GitHub
- [x] 完成 10 分钟英语阅读和 3 句摘抄

二、今日产出

- 接口地址：http://localhost:8080/hello
  Git commit：Init Spring Boot project and add hello endpoint

- 踩坑与解决记录：

  遇到问题：IntelliJ IDEA 使用的 Maven Wrapper 版本与IDEA自带的Maven集成不兼容。

   

  解决办法：

  第一步：修改IDEA的Maven配置

  修改Maven home path

  将原来的Use Maven Wrapper改成Bundled Maven 3（如果显示具体版本号，就选择IDEA自带的Maven 3.x）。

  第二步：重新加载项目



【Day 3 打卡】

 日期：2026/10/11

一、今日完成

- [x] 创建数据库
- [x] 创建 sys_user 表
- [x] 项目连接 MySQL 成功
- [x] 创建 SysUser 实体
- [x] 创建 SysUserMapper
- [x] 测试查询或插入成功
- [x]  已提交 Git

二、今日问题

1.配置文件：application.properties 与配置文件：application.yml格式不同，注意区分



【Day 4 打卡】

日期：2026/10/12

一、今日完成
- [x] 完成 /api/user/register
- [x] 用户名重复校验
- [x] 参数非空校验
- [x] Postman 测试成功
- [x] 已提交 Git

二、今日问题

1.MyBatis-Plus里看到lambdaQuery，它通常指：Wrappers.lambdaQuery()也就是创建一个LambdaQueryWrapper，用来构建类型安全的查询条件。

 

2.Wrappers.<SysUser>lambdaQuery()用Lambda方法引用构建查询条件的工具，返回LambdaQueryWrapper，比字符串写字段名更安全、更优雅。

 

3.:: 是Java的方法引用语法：在MyBatis-Plus里，User::getName的意思是：引用User类的getName方法，让框架推导出字段name

 

4.为什么直接URL?后面加参数无法查询？

因为我的Controller方法参数前面加了@RequestBody，它只认请求体（Body），不认URL查询参数（Query Params）。

@RequestParam  URL?后面 一般用于查询条件，如/list?page=1&size=10。

@RequestBody  安全，标准写法

 

5.isEmpty()和isBlank()的区别,isBlank是java11专门用于判断字符串是不是空白字符串。可以排除掉空格



【Day 5 打卡】

一、今日完成
- [x] 登录接口完成
- [x] 登录成功返回 Token
- [x] 简单 Token 存储完成
- [x] /api/user/me 可获取当前用户
- [x] 拦截器可拦截无效 Token
- [x] Postman 测试成功
- [x] 已提交 Git

二、今日问题

1.关键点：request.getHeader("token")：从请求头读取Token。

​                   tokenService.getUserId(token)：检查Token是否存在。走拦截器的逻辑

​                   request.setAttribute("userId", userId)：把服务端验证过的用户ID传给Controller

2.键值对保存token，目前先使用内存存储

（缺点：重启丢失，多实例不同步；如果项目部署在两台服务器上，两台服务器的 Map 不会自动共享；无法自动过期：你当前的代码没有设置过期时间）

ConcurrentHashMap：支持并发安全访问的键值对集合。

tokenStore：你在这个项目中用来保存 Token → 用户ID关系的具体变量。



【Day 6 打卡】

2026/10/16

一、今日完成
- [x] 创建 file_info 表
- [x] 完成 /api/file/upload
- [x] 文件能保存到本地
- [x] 数据库能记录文件信息
- [x] 需要 Token 才能上传
- [x] 完善英文 README

- [x] 创建安全审计 Checklist
- [x] 创建漏洞记录文档

二、问题

1.没有直接使用用户提供的原始文件名作为磁盘文件名，而是单独设置 stored_name，后续可以使用随机 UUID 生成存储名，降低文件名冲突和路径穿越风险

2.

@Mapper
public interface FileInfoMapper extends BaseMapper<FileInfo> {}中为什么里面没有写SQL？
因为BaseMapper<FileInfo>已经提供了常用数据库操作



2026/10/18

一、今日完成

- [x] 确认 file_info 数据表和 FileInfo 实体类。


- [x] 编写 **GET /api/file/list 接口**。


- [x]  从请求中的 Token 获取当前用户的 userId。


- [x]  查询数据库，只返回当前用户上传的文件。


- [x]  统一使用项目中的 Result 封装响应。

- [x] 完成**文件下载接口 Get  /api/file/download/{fileId}**

- [x] 使用 Postman 测试文件下载

- [x] 文件下载的身份验证与文件权限控制

二、问题

1.**Authorization: Bearer** 4287c22b-24ed-4e23-90af-eb91667ed20f

Authorization  ---->  HTTP 请求头的名称，表示提供身份认证凭据

Bearer   ---->  认证方案，表示后面跟着一个持有者 Token

4287c22b-24ed-4e23-90af-eb91667ed20f   ---->    Token

这是一种常见的 **API 认证约定**，但你的后端也可以设计成其他格式。关键是客户端和服务器必须约定一致。后接入 Spring Security 或 JWT 时，也很容易理解和迁移。

2.下载文件关键

身份认证： 通过 Token 确定当前用户是谁。

权限控制： 查询时同时匹配 fileId 和 userId，避免越权下载。

文件路径限制： 只允许读取配置上传目录中的真实文件。



2026/10/19

一、今日完成

- [x] 编写文件删除接口 **DELETE /api/file/{id}**


- [x] 实现当前用户身份校验


- [x] 实现文件所有权检查


- [x] 实现磁盘文件删除


- [x] 实现数据库文件记录删除


- [x] 检查文件路径，防止路径遍历


- [x] 确认删除接口受到 Token 拦截器保护
- [x] 检查越权情况
- [x] 使用 BCrypt 存储密码哈希
- [x] 增加token过期和注销机制

二 、问题

1.现在先删除磁盘文件、再删除数据库记录的顺序可能导致两者不一致：例如磁盘文件已删除，但数据库操作失败。

**对于当前学习项目，先记录这一风险，后续再学习事务补偿或文件清理机制**。

2.上传接口、下载接口和删除接口应使用相同的文件定位规则。否则可能出现上传成功，但下载或删除时找不到文件的情况。

3.
GET /api/file/list	                          ----> 验证 Token，再调用 listByUserId(userId)	 
POST /api/file/upload	                ----> 从拦截器获取 userId，保存文件归属	 
GET /api/file/download/{fileId}	----> 验证 Token，再调用 getOwnedFile(fileId, userId)	 
DELETE /api/file/{id}	                  ----> 查询时同时限制文件 ID 和用户 ID	 
GET /api/user/me	                      ----> 通过请求属性获取用户 ID	 
GET /api/user/list	                      ----> 直接查询并返回所有 SysUser	 
POST /api/user/register	           ----> 公开注册接口	 
POST /api/user/login	                ----> 验证用户名和密码后签发 Token	  

4.fileservice的权限

listByUserId(userId)
    └── 只查询当前用户的文件

getOwnedFile(fileId, userId)
    └── 只查询当前用户拥有的指定文件

deleteOwnedFile(fileId, userId)
    └── 只删除当前用户拥有的指定文件记录

5.BCrypt 的特点是：
- 相同的密码多次哈希，得到的结果通常不同，因为每次都会生成随机盐值。
- 不能像普通编码那样解密还原原始密码。
- 验证密码时，应调用 matches()，而不是将新生成的哈希与数据库中的字符串直接比较。

6.过期机制解决的是“到时间自动失效”，注销机制解决的是“用户主动要求失效”。

7.当前 Token 数据存在 `ConcurrentHashMap` 中，只适合单实例学习项目。如果未来部署多台应用服务器，每台服务器会拥有不同的 Token 表；届时需要使用 Redis 等共享存储，并利用过期时间和删除操作实现跨实例的统一失效。

8.账户表：QQ/1111;tom/123456



2026/10/21

一、今日完成

- [x] 完成security-test.md



上传限制，1.在application.yml 的 Multipart 限制

​                   2.在FileController代码层做校验



2026/10/24

一、今日完成

- [x] 完成上传测试，fileuploadcheck.md

- [x] 完成路径穿越测试，pathtraversalcheck.md



2026/10/25

一、今日完成

- [x] 完成登录限制的设计，增加LoginAttemptService，用时间戳记录
- [x] 测试验证连续失败达到阈值后被限制，返回"code": 429, "message": "登录尝试过于频繁，请稍后再试"
- [x] 验证限制期间正确密码仍被拒绝，返回"code": 429, "message": "登录尝试过于频繁，请稍后再试"
- [x] 验证限制期结束后恢复登录，返回"code": 200
- [x] 验证正常登录与 Token 功能没有回归问题，测试其他接口使用正常



2026/11/5

## 使用OWASP Dependency-Check 扫描 Maven 依赖

1.在 `pom.xml` 的 `build/plugins` 中添加 OWASP Dependency-Check 插件：

```xml
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>13.0.0</version>
    <configuration>
        <formats>
            <format>HTML</format>
            <format>JSON</format>
        </formats>
        <outputDirectory>
            ${project.build.directory}/dependency-check
        </outputDirectory>
    </configuration>
</plugin>
```

//随后执行绑定 Maven 生命周期 

```shell
mvn clean verify
```

//随后执行依赖扫描

```bash
mvn org.owasp:dependency-check-maven:13.0.0:check
```

### --遇到问题1

OWASP Dependency-Check 在下载 RetireJS 漏洞数据库时，网络连接被重置，导致扫描初始化失败。RetireJS 的数据库需要从 GitHub 下载。如果当前网络无法访问 `raw.githubusercontent.com`，就可能出现这个问题。

1.配置当前 PowerShell 会话

将下面的端口替换成代理软件实际 HTTP 端口：

```shell
$env:JAVA_TOOL_OPTIONS="-Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=代理端口 -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=代理端口"
```

2.配置maven，C:\Users\98292\.m2\settings.xml

```xml
<settings>
    <proxies>
        <proxy>
            <id>local-proxy</id>
            <active>true</active>
            <protocol>http</protocol>
            <host>127.0.0.1</host>
            <port>7897</port>
            <nonProxyHosts>localhost|127.0.0.1</nonProxyHosts>
        </proxy>
    </proxies>
</settings>
```

3.用Dependency-Check 官方提供的 Maven 代理配置方式

```shell
mvn -DmavenSettingsProxyId=local-proxy org.owasp:dependency-check-maven:13.0.0:check
```

### --遇到问题2

NVD API Key 为空

1.打开这个网站申请api密钥https://nvd.nist.gov/developers/request-an-api-key?utm_source=chatgpt.com

2.设置你的密钥

```shell
$env:NVD_API_KEY = "这里填写你申请到的真实API Key"
```

3.修改原来的pom文件

```xml
<configuration>
    <!-- 从环境变量读取 NVD API Key，不把密钥写入 pom.xml -->
    <nvdApiKeyEnvironmentVariable>
        NVD_API_KEY
    </nvdApiKeyEnvironmentVariable>
    <formats>
        <format>HTML</format>
        <format>JSON</format>
    </formats>
    <outputDirectory>
        ${project.build.directory}/dependency-check
    </outputDirectory>
</configuration>
```

### 修改好两个问题后，最后在配置好的powershell界面运行

```shell
mvn -DmavenSettingsProxyId=local-proxy org.owasp:dependency-check-maven:13.0.0:check
```



建议你以后分析 Dependency-Check 报告，都按照下面这套顺序走。

| 步骤            | 你要回答的问题             | 主要检查内容               |
| --------------- | -------------------------- | -------------------------- |
| 1. 找组件       | 哪个依赖有问题？           | GroupId、ArtifactId        |
| 2. 确认版本     | 项目究竟用了哪个版本？     | Dependency Tree            |
| 3. 核对 CVE     | 这个漏洞到底是什么？       | 漏洞描述、厂商公告         |
| 4. 核对版本范围 | 当前版本是否命中？         | Affected Versions          |
| 5. 分析攻击条件 | 攻击者需要满足什么条件？   | 网络入口、认证、配置、功能 |
| 6. 分析项目影响 | 项目是否真的使用相关功能？ | 配置文件、代码、部署架构   |
| 7. 确认整改     | 有没有官方修复版本？       | Fixed Version              |
| 8. 复测         | 漏洞是否已经消除？         | 更新依赖、重新扫描         |

还有一个特别重要的原则：

依赖存在漏洞 ≠ 项目一定可以被利用；项目暂时无法触发漏洞 ≠ 不需要整改。

你需要同时保留两个判断：

- 依赖风险：版本是否命中漏洞范围。
- 实际风险：项目是否具备对应的攻击条件。

## 七、这次可以形成一份真正的审计记录

建议将这条漏洞记录在你的 Dependency-Check 分析报告中：

| 项目         | 本次分析结果                                           |
| ------------ | ------------------------------------------------------ |
| 漏洞编号     | CVE-2026-65637                                         |
| 弱点分类     | CWE-20                                                 |
| 受影响组件   | tomcat-embed-core                                      |
| 项目当前版本 | 11.0.24                                                |
| 受影响版本   | 11.0.20～11.0.24                                       |
| 修复版本     | 11.0.25 及以上                                         |
| 实际攻击条件 | HTTP/2、严格 SNI 校验及相关虚拟主机配置                |
| 当前判断     | 依赖版本命中，实际可利用性待配置核查                   |
| 整改建议     | 升级 Spring Boot 至 4.1.2，使用其托管的 Tomcat 11.0.27 |
| 复测方式     | Maven 依赖树 + Dependency-Check 重新扫描               |

我建议你现在先不要急着处理报告里的所有漏洞。

先完成这条漏洞的完整闭环：版本确认 → 配置分析 → 升级 → 重新扫描 → 记录整改结果。

这样你学到的就不只是 Dependency-Check 工具的使用方法，而是真正的 Java 依赖漏洞审计流程。

下一步可以把你的 `application.yml` 和 `pom.xml` 发来，我帮你核对 HTTP/2、TLS 配置以及 Spring Boot 升级位置。
