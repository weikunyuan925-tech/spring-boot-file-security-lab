# 文件上传安全审计报告

## 1. 基本信息

- 项目：spring-boot-file-security-lab
- 审计模块：文件上传接口
- 接口：POST /api/file/upload
- 测试环境：本地 Spring Boot + MySQL + Postman
- 审计日期：2026/10/22-2026/10/23
- 代码版本或 Git Commit：7d83e7f
- 测试人员：ikun

## 2. 审计目标

检查文件上传接口对文件大小、文件类型、原始文件名和磁盘存储路径的校验能力，验证非法上传是否会被拒绝，以及失败请求是否会造成数据库记录或磁盘文件残留。

## 3. 审计范围与规则

- 单个文件最大大小：10 MiB
- 允许的扩展名：.pdf、.png、.jpg、.jpeg
- 文件类型验证：扩展名白名单与实际文件签名匹配
- 原始文件名：限制长度并处理路径信息和非法字符
- 磁盘存储名：由服务器生成 UUID
- 存储位置：确认在Web公共目录之外

## 4. 现有实现检查

| 检查项                      | 当前结论       | 证据或待确认事项                                             |
| --------------------------- | -------------- | ------------------------------------------------------------ |
| 文件大小限制                | 已实现         | SIZE-02 已验证超大文件拒绝；补充实际 HTTP 状态码             |
| 扩展名白名单                | 已实现         | TYPE-04 中 `.txt`、`.html`、`.rar`、`.jsp`、`.exe` 被拒绝    |
| 实际文件内容验证            | 已实现基础检查 | TYPE-05、TYPE-06 通过；属于签名检测，不等于完整文件格式解析  |
| 客户端 MIME 信任问题        | 初步验证通过   | TYPE-07 已拒绝文本伪装 PNG；建议保持文件内容相同，仅改变 Content-Type 做对照 |
| 原始文件名规范化            | 已实现         | NAME-02 将 `../../empty.pdf` 处理为 `empty.pdf`              |
| 文件名长度限制              | 已实现         | NAME-03 超过 255 字符后被拒绝；补充实际 HTTP 状态码          |
| 控制字符及非法字符          | 部分验证通过   | 冒号、换行符已有证据；确认制表符测试也已通过                 |
| 随机存储名与防覆盖          | 已验证基本行为 | NAME-05 原文件内容不变；补充数据库记录与 UUID 文件路径       |
| 上传目录在 Web 公共目录之外 | 尚需确认       | 需要检查实际运行目录、上传目录配置和静态资源映射，当前测试记录不足以证明这一点 |

## 5. 测试结果

| 用例编号 | 测试内容                                                     | 实际结果                                                     | 证据                                                         | 结论       |
| -------- | ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ | ---------- |
| SIZE-01  | 正常大小的有效文件                                           | 登录账户QQ，token：01bddd83-1f7d-4920-a81a-c8657807bf57 <br/>访问POST：http://localhost:8080/api/file/upload，上传文件ikun.png<br/>{<br/>    "code": 200,<br/>    "message": "上传成功",<br/>    "data": {<br/>        "id": 15,<br/>        "originalName": "ikun.png",<br/>        "fileSize": 387809,<br/>        "storedName": "5d5d2c13-b371-4143-bf25-14644915a632.png"<br/>    }<br/> | ![](../picture/0.png)                                        | 正常       |
| SIZE-02  | 超过最大大小                                                 | 登录账户QQ，token：01bddd83-1f7d-4920-a81a-c8657807bf57 <br/>访问POST：http://localhost:8080/api/file/upload，上传文件girltupian.jpg<br/>{<br/>    "code": 413,<br/>    "message": "文件大小超过限制",<br/>    "data": null<br/>}<br/> | 未写入磁盘中                                                 | 正常       |
| SIZE-03  | 上传空文件                                                   | 登录账户QQ，token：01bddd83-1f7d-4920-a81a-c8657807bf57 <br/>访问POST：http://localhost:8080/api/file/upload，上传文件empty.png<br/>{<br/>    "code": 400,<br/>    "message": "请选择有效的文件",<br/>    "data": null<br/>} | 未写入磁盘中                                                 | 正常       |
| TYPE-01  | 有效 PDF 文件                                                | 登录账户QQ，token：01bddd83-1f7d-4920-a81a-c8657807bf57 <br/>访问POST：http://localhost:8080/api/file/upload，上传文件红队常用攻击方法.pdf<br/>{<br/>    "code": 200,<br/>    "message": "上传成功",<br/>    "data": {<br/>        "id": 16,<br/>        "originalName": "红队常用攻击方法.pdf",<br/>        "fileSize": 310724,<br/>        "storedName": "f5396727-4b27-4b23-87aa-0969b0f09ed7.pdf"<br/>    }<br/>} | ![](../picture/1.png)                                        | 正常       |
| TYPE-02  | 上传有效 PNG 文件，使用 .png 扩展名                          | 登录账户QQ，token：01bddd83-1f7d-4920-a81a-c8657807bf57 <br/>访问POST：http://localhost:8080/api/file/upload，图片1.png<br/>{<br/>    "code": 200,<br/>    "message": "上传成功",<br/>    "data": {<br/>        "id": 18,<br/>        "originalName": "图片1.png",<br/>        "fileSize": 1719293,<br/>        "storedName": "79c63360-5eb9-4251-bf1e-e4def3c21d7b.png"<br/>    }<br/>} | ![](../picture/3.png)                                        | 正常       |
| TYPE-03  | 上传有效 JPEG 文件，使用 .jpg 或 .jpeg 扩展名                | 登录账户QQ，token：01bddd83-1f7d-4920-a81a-c8657807bf57 <br/>访问POST：http://localhost:8080/api/file/upload，上传文件R.jpg<br/>{<br/>    "code": 200,<br/>    "message": "上传成功",<br/>    "data": {<br/>        "id": 19,<br/>        "originalName": "R.jpg",<br/>        "fileSize": 162802,<br/>        "storedName": "13f9ed30-135d-4d5b-ac72-dd6b2835299a.jpg"<br/>    }<br/>} | ![](../picture/4.png)                                        | 正常       |
| TYPE-04  | 禁止的扩展名上传 .exe、.jsp 或 .html、txt'文件               | 登录账户QQ，token：01bddd83-1f7d-4920-a81a-c8657807bf57 <br/>访问POST：http://localhost:8080/api/file/upload，上传文件testqq.txt、saved_resource.html、十四.rar、十四 - 副本 (2).jsp、十四 - 副本.exe<br/><br />均返回以下响应<br />{<br/>    "code": 400,<br/>    "message": "不支持的文件类型",<br/>    "data": null<br/>} | 未写入磁盘中                                                 | 正常       |
| TYPE-05  | 文本文件伪装成 PNG                                           | 登录账户QQ，token：01bddd83-1f7d-4920-a81a-c8657807bf57 <br/>访问POST：http://localhost:8080/api/file/upload，上传文件testqq - 改为png.png<br/>{<br/>    "code": 400,<br/>    "message": "无法识别或不支持的文件内容",<br/>    "data": null<br/>} | 本来文件为txt文件，改了文件后缀为png，校验不通过，未写入磁盘中 | 正常       |
| TYPE-06  | 将 PDF 文件改名为 fake.png                                   | 登录账户QQ，token：01bddd83-1f7d-4920-a81a-c8657807bf57 <br/>访问POST：http://localhost:8080/api/file/upload，上传文件红队常用攻击方法 - 有内容pdf改png.png<br/>{<br/>    "code": 400,<br/>    "message": "文件扩展名与实际内容不匹配",<br/>    "data": null<br/>} | 本来文件为pdf文件，改了文件后缀为png，校验不通过，未写入磁盘中 | 正常       |
| TYPE-07  | 使用与真实内容不符的客户端MIME声明上传                       | 登录账户QQ，token：01bddd83-1f7d-4920-a81a-c8657807bf57 <br/>访问POST：http://localhost:8080/api/file/upload，上传文件testqq - 改为png.png，将Content Type 列，将它手动设置为 image/png<br/>{<br/>    "code": 400,<br/>    "message": "无法识别或不支持的文件内容",<br/>    "data": null<br/>} | 不依赖于客户端MIME声明上传，Content Type列，将它手动设置为 image/png无法通过校验，除非本来就是符合条件的文件 | 正常       |
| NAME-01  | 上传有效文件名的pdf                                          | 登录账户QQ，token：01bddd83-1f7d-4920-a81a-c8657807bf57 <br/>访问POST：http://localhost:8080/api/file/upload，上传文件红队常用攻击方法.pdf<br />{<br/>    "code": 200,<br/>    "message": "上传成功",<br/>    "data": {<br/>        "storedName": "21e3416f-1af7-4452-b149-ee0885498af2.pdf",<br/>        "fileSize": 310724,<br/>        "originalName": "红队常用攻击方法.pdf",<br/>        "id": 24<br/>    }<br/>} | 正常上传                                                     | 正常       |
| NAME-02  | 使用路径形式的文件名，例如 ../../outside.pdf                 | 登录账户QQ，token：01bddd83-1f7d-4920-a81a-c8657807bf57 <br />因为window限制文件名有符号，用powershell放送请求<br />curl.exe -i <br/>  -H "Authorization: Bearer 4ab1ca2e-7352-4c77-920c-e48ebe5cae80"<br/>  -F "file=@C:\Users\98292\Desktop\素材库\empty.pdf;filename=**../../empty.pdf**;type=application/pdf" `<br/>  "http://localhost:8080/api/file/upload"<br /><br />{"code":200,"message":"上传成功","data":{"storedName":"a688bf02-258d-4212-bbf1-4f25803211f4.pdf","fileSize":1305,"originalName":"empty.pdf","id":26}} | 采用了规范化策略： ../../empty.pdf 处理成empty.pdf，然后通过检查，上传成功![](../picture/5.png) | 正常       |
| NAME-03  | 上传超过 255 个字符的文件名                                  | 登录账户QQ，token：01bddd83-1f7d-4920-a81a-c8657807bf57 <br />因为window限制文件名超过255字符，所有用powershell来测试<br/><br/>$name = ("a" * 260) + ".pdf"<br/><br/>curl.exe -i <br/>  -H "Authorization: Bearer 64fd695e-e0de-4849-a3b3-d68884227108"<br/>  -F "file=@C:\Users\98292\Desktop\素材库\empty.pdf;filename=$name;type=application/pdf" `<br/>  "http://localhost:8080/api/file/upload"<br/><br />{"code":400,"message":"文件名非法或长度超出限制","data":null} | 做了限制，无法上传文件                                       | 正常       |
| NAME-04  | 使用包含控制字符或非法特殊字符的文件名                       | 登录账户QQ，token：01bddd83-1f7d-4920-a81a-c8657807bf57 <br />因为window限制文件名非法特殊字符，所有用powershell来测试<br/><br/>curl.exe -i <br/>  -H "Authorization: Bearer bb84eda7-8868-41ea-8ef9-4a13611b07d8"<br/>  -F "file=@C:\Users\98292\Desktop\素材库\empty.pdf;filename=bad:name.pdf;type=application/pdf" `<br/>  "http://localhost:8080/api/file/upload"<br /><br />{"code":400,"message":"文件名非法或长度超出限制","data":null}<br /><br />------------------------------------------------------------------------------------------------<br />1.在 Controller 相同包名的测试类：com.example.filelab.controller.FileControllerTest<br />2.让测试能够直接调用方法，将 sanitizeOriginalName() 的访问修饰符从 private 暂时改为包级可见，即删除 private；测试类与 Controller 必须在同一个包中<br />3.class FileControllerTest {<br/><br/>    @Test<br/>    void shouldRejectFilenameWithNewline() {<br/>        FileController controller =<br/>                new FileController(null, null, null);<br/><br/>        String result =<br/>                controller.sanitizeOriginalName("bad\nname.pdf");<br/><br/>        assertNull(result);<br/>    }<br/><br/>    @Test<br/>    void shouldRejectFilenameWithTab() {<br/>        FileController controller =<br/>                new FileController(null, null, null);<br/><br/>        String result =<br/>                controller.sanitizeOriginalName("bad\tname.pdf");<br/><br/>        assertNull(result);<br/>    }<br/>} | 做了限制，无法上传文件<br />![](../picture/6.png)            | 正常       |
| NAME-05  | 用户即使上传允许的 `.pdf` 文件，并故意把文件名设置成服务器上已有的文件名，也不能覆盖原有文件。 | 1.先是powershell，创建一个测试文件在上传目录位置<br />$uploadDir = "D:\spring-boot-file-security-lab\uploads" <br />$sentinel = Join-Path $uploadDir "**overwrite-test.pdf**"  <br />if (Test-Path $sentinel) <br />{     throw "测试目标文件已经存在，请先更换测试文件名，避免覆盖现有文件。" } <br /> New-Item -ItemType Directory -Force -Path $uploadDir \| Out-Null  Set-Content -Path <br />$sentinel <br />-Value "DO_NOT_OVERWRITE"    <br />    -NoNewline  <br />Get-Content $sentinel<br />这是标记一个测试文件，用来测试的文件会不会被覆盖<br />2.上传一个同名文件overwrite-test.pdf<br />curl.exe -i <br/>  -H "Authorization: Bearer  有效Token"<br/>  -F "file=@C:\Users\98292\Desktop\素材库\empty.pdf;filename=**overwrite-test.pdf**;type=application/pdf" `<br/>  "http://localhost:8080/api/file/upload"<br />3.检查原来文件内容有没有被覆盖<br />Get-Content $sentinel<br />发现内容不变，并且磁盘上传的目录里有新生产的一个文件，包含由uuid开头的一个文件。<br />4.没有进行覆盖，通过 | ![](../picture/7.png)1.原有 overwrite-test.pdf 的内容仍然是 DO_NOT_OVERWRITE。<br/>2.本次上传成功后，数据库存在对应的新记录。<br/>3.新记录中的 stored_name 是 UUID 文件名。<br/>4.file_path 指向上传目录中的新文件。<br/>5.原文件没有被覆盖，新文件没有写入上传目录之外。 | 正常无覆盖 |



## 8. 审计结论

本次审计针对 `POST /api/file/upload` 开展文件大小、扩展名白名单、实际文件内容、客户端 MIME 声明、原始文件名及磁盘存储名等安全检查。

当前测试结果表明，服务端已具备以下基础防护措施：

1. 对超过配置大小限制的文件和空文件进行拒绝处理。
2. 使用扩展名白名单限制允许上传的文件格式。
3. 根据文件实际内容进行基础签名识别，并校验扩展名与实际类型是否匹配。
4. 对原始文件名中的路径信息、超长字符串、控制字符和部分非法字符进行处理。
5. 使用 UUID 生成实际磁盘存储名，降低客户端控制路径和覆盖已有文件的风险。

已记录的 TYPE-05、TYPE-06、NAME-02、NAME-03 和 NAME-05 测试分别验证了实际内容校验、文件名规范化、长度限制和同名文件防覆盖行为。

仍需补充的工作包括：确认关键用例的真实 HTTP 状态码，完成 MIME 声明对照测试，确认所有文件名单元测试均已通过，以及提供上传目录不位于 Web 公共目录中的配置证据。

本报告的结论仅适用于已检查的上传接口和本次测试环境。基础文件签名检测不等同于完整文件格式解析或恶意文件扫描，不能据此认定所有上传安全风险均已消除。