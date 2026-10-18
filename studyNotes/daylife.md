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



