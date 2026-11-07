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



Dependency-Check报告模板。

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

还有一个特别重要的原则：依赖存在漏洞 ≠ 项目一定可以被利用；项目暂时无法触发漏洞 ≠ 不需要整改。

你需要同时保留两个判断：**1.依赖风险：版本是否命中漏洞范围。2.实际风险：项目是否具备对应的攻击条件。**



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



| 项目         | 本次分析结果                      |
| ------------ | --------------------------------- |
| 漏洞编号     | CVE-2026-60586                    |
| 弱点分类     | CWE-306（缺少关键功能的身份验证） |
| 受影响组件   | mysql-connector-j                 |
| 项目当前版本 | 9.7.0                             |
| 受影响版本   | 9.7.0-9.7.1                       |
| 修复版本     | 暂无                              |
| 实际攻击条件 | 缺少关键功能的身份验证            |
| 当前判断     | 尚待分析                          |
| 整改建议     | 待整改                            |
| 复测方式     | 暂无                              |