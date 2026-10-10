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
