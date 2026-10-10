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

​  3.每次更新都要

​      git add .
​      git commit -m "写清楚你改了什么"
​      git push
