

| 接口                            | 检查的重点                     |
| ------------------------------- | ------------------------------ |
| POST /api/user/register         | 能否正常创建测试用户           |
| POST /api/user/login            | 能否获得 Token                 |
| GET /api/user/me                | Token 是否能识别正确的用户     |
| GET /api/user/list              | 普通用户是否被禁止查看全体用户 |
| GET /api/file/list              | 是否只返回当前用户的文件       |
| POST /api/file/upload           | 上传文件是否正确记录归属用户   |
| GET /api/file/download/{fileId} | 是否能阻止跨用户下载           |
| DELETE /api/file/{id}           | 是否能阻止跨用户删除           |

测试账户

1.QQ/1111        token：0d5803f4-1036-4d64-b290-7dc99c38f8ce

2.tom/123456   token：54665e91-6634-4345-8167-9d2f87881a15



UserAuthentication

| 测试用例 | AUTH-01                                                      |
| -------- | ------------------------------------------------------------ |
| 测试目标 | 不带 Token，访问 /api/user/me                                |
| 前置条件 | 测试账户：QQ/1111                                            |
| 测试数据 | get：http://localhost:8080/api/user/me                       |
| 实际结果 | {<br/>    "message": "请提供有效的 Token",<br/>    "data": {},<br/>    "code": 401<br/>} |
| 验证证据 | 账户存在，无法进入                                           |
| 测试结论 | 成功                                                         |



| 测试用例 | AUTH-02                                                      |
| -------- | ------------------------------------------------------------ |
| 测试目标 | 携带随机无效 Token，访问 /api/user/me                        |
| 前置条件 | 测试账户：QQ/1111，token：e560392e-3660-4a88-a4c6-ae618e53b970 |
| 测试数据 | get：http://localhost:8080/api/user/me                       |
| 实际结果 | {<br/>    "message": "Token 无效或已失效",<br/>    "data": {},<br/>    "code": 401<br/>} |
| 验证证据 | 账户存在，无法进入                                           |
| 测试结论 | 成功                                                         |



| 测试用例 | AUTH-03                                                      |
| -------- | ------------------------------------------------------------ |
| 测试目标 | 携带 Token A，访问 /api/user/me                              |
| 前置条件 | 测试账户：QQ/1111，token：e560392e-3660-4a88-a4c6-ae618e53b970 |
| 测试数据 | get：http://localhost:8080/api/user/me                       |
| 实际结果 | {<br/>    "code": 200,<br/>    "message": "获取成功",<br/>    "data": {<br/>        "id": 2112196725069844481,<br/>        "username": "QQ",<br/>        "role": "USER"<br/>    }<br/>} |
| 验证证据 | 账户存在，查询到qq的信息                                     |
| 测试结论 | 成功                                                         |



| 测试用例 | AUTH-04                                                      |
| -------- | ------------------------------------------------------------ |
| 测试目标 | 携带 Token B，访问 /api/user/me                              |
| 前置条件 | 测试账户：tom/123456   token：705eb3ce-9ac0-4ea4-9435-a99a35b563d0 |
| 测试数据 | get：http://localhost:8080/api/user/me                       |
| 实际结果 | {<br/>    "code": 200,<br/>    "message": "获取成功",<br/>    "data": {<br/>        "id": 2112203056111685633,<br/>        "username": "tom",<br/>        "role": "USER"<br/>    }<br/>} |
| 验证证据 | 账户存在，查询到tom的信息                                    |
| 测试结论 | 成功                                                         |



File Authorization

| 测试用例 | AUTH-05                                                      |
| -------- | ------------------------------------------------------------ |
| 测试目标 | 不带 Token 上传文件                                          |
| 前置条件 | 测试账户：tom/123456   token：无                             |
| 测试数据 | post：http://localhost:8080/api/file/upload                  |
| 实际结果 | {<br/>    "message": "请提供有效的 Token",<br/>    "data": {},<br/>    "code": 401<br/>} |
| 验证证据 | 账户存在，无法上传                                           |
| 测试结论 | 成功                                                         |



| 测试用例 | AUTH-06                                                      |
| -------- | ------------------------------------------------------------ |
| 测试目标 | 使用无效 Token 上传文件                                      |
| 前置条件 | 测试账户：tom/123456   token：test-705eb3ce-9ac0-4ea4-9435-a99a35b563d0 |
| 测试数据 | post：http://localhost:8080/api/file/upload                  |
| 实际结果 | {<br/>    "message": "Token 无效或已失效",<br/>    "data": {},<br/>    "code": 401<br/>} |
| 验证证据 | 账户存在，无法上传                                           |
| 测试结论 | 成功                                                         |



| 测试用例 | AUTH-07                                                      |
| -------- | ------------------------------------------------------------ |
| 测试目标 | 不带 Token 下载文件                                          |
| 前置条件 | 测试账户：tom/123456   token：无                             |
| 测试数据 | get：http://localhost:8080/api/file/download/8               |
| 实际结果 | {<br/>    "message": "请提供有效的 Token",<br/>    "data": {},<br/>    "code": 401<br/>} |
| 验证证据 | 账户存在，无法下载                                           |
| 测试结论 | 成功                                                         |



| 测试用例 | AUTH-08                                                      |
| -------- | ------------------------------------------------------------ |
| 测试目标 | 不带 Token 删除文件                                          |
| 前置条件 | 测试账户：tom/123456   token：无                             |
| 测试数据 | delete：http://localhost:8080/api/file/8                     |
| 实际结果 | {<br/>    "message": "请提供有效的 Token",<br/>    "data": {},<br/>    "code": 401<br/>} |
| 验证证据 | 未携带 Token，删除请求被拒绝；随后确认数据库记录和磁盘文件仍然存在。 |
| 测试结论 | 成功                                                         |



tom：d0eebc61-d316-4d72-9ca3-e0290ed8a54f

QQ:8581887d-0812-4fd3-9c5c-d29e648db741



| 测试用例 | FILE-01                                                      |
| -------- | ------------------------------------------------------------ |
| 测试目标 | 用户 A 查看文件列表                                          |
| 前置条件 | 测试账户：QQ   token：8581887d-0812-4fd3-9c5c-d29e648db741   |
| 测试数据 | GET:http://localhost:8080/api/file/list                      |
| 实际结果 | {<br/>    "code": 200,<br/>    "message": "success",<br/>    "data": [<br/>        {<br/>            "id": 13,<br/>            "fileName": "testqq2.txt",<br/>            "fileSize": 31,<br/>            "createTime": "2026-10-20T19:09:28"<br/>        },<br/>        {<br/>            "id": 12,<br/>            "fileName": "testqq.txt",<br/>            "fileSize": 8,<br/>            "createTime": "2026-10-20T19:09:21"<br/>        },<br/>        {<br/>            "id": 11,<br/>            "fileName": "test1020.txt",<br/>            "fileSize": 31,<br/>            "createTime": "2026-10-20T19:09:15"<br/>        }<br/>    ]<br/>} |
| 验证证据 | 只查看当前QQ的账户信息。                                     |
| 测试结论 | 成功                                                         |



| 测试用例 | FILE-02                                                      |
| -------- | ------------------------------------------------------------ |
| 测试目标 | 用户 B 查看文件列表                                          |
| 前置条件 | 测试账户：tom   token：d0eebc61-d316-4d72-9ca3-e0290ed8a54f  |
| 测试数据 | GET:http://localhost:8080/api/file/list                      |
| 实际结果 | {<br/>    "code": 200,<br/>    "message": "success",<br/>    "data": [<br/>        {<br/>            "id": 10,<br/>            "fileName": "testtom2.txt",<br/>            "fileSize": 31,<br/>            "createTime": "2026-10-20T19:08:54"<br/>        },<br/>        {<br/>            "id": 9,<br/>            "fileName": "testtom.txt",<br/>            "fileSize": 31,<br/>            "createTime": "2026-10-20T19:08:46"<br/>        },<br/>        {<br/>            "id": 8,<br/>            "fileName": "test1018.txt",<br/>            "fileSize": 31,<br/>            "createTime": "2026-10-20T19:05:23"<br/>        }<br/>    ]<br/>} |
| 验证证据 | 只查看当前tom的账户信息。                                    |
| 测试结论 | 成功                                                         |



| 测试用例 | FILE-03                                                     |
| -------- | ----------------------------------------------------------- |
| 测试目标 | 下载QQ的文件                                                |
| 前置条件 | 测试账户：tom   token：d0eebc61-d316-4d72-9ca3-e0290ed8a54f |
| 测试数据 | GET:http://localhost:8080/api/file/download/8               |
| 实际结果 | 下载成功                                                    |
| 验证证据 | 返回文件数据                                                |
| 测试结论 | 成功                                                        |



| 测试用例 | FILE-04                                                    |
| -------- | ---------------------------------------------------------- |
| 测试目标 | 下载tom的文件                                              |
| 前置条件 | 测试账户：QQ   token：8581887d-0812-4fd3-9c5c-d29e648db741 |
| 测试数据 | GET:http://localhost:8080/api/file/download/12             |
| 实际结果 | 下载成功                                                   |
| 验证证据 | 返回文件数据                                               |
| 测试结论 | 成功                                                       |



| 测试用例 | FILE-05                                                      |
| -------- | ------------------------------------------------------------ |
| 测试目标 | 用户 B 携带自己的 Token 下载用户 A 的文件 ID                 |
| 前置条件 | 测试账户：QQ   token：8581887d-0812-4fd3-9c5c-d29e648db741 下载tom的文件，fildid8 |
| 测试数据 | GET:http://localhost:8080/api/file/download/8                |
| 实际结果 | {<br/>    "code": 404,<br/>    "message": "文件不存在",<br/>    "data": null<br/>} |
| 验证证据 | 无法下载                                                     |
| 测试结论 | 成功                                                         |



| 测试用例 | FILE-06                                                      |
| -------- | ------------------------------------------------------------ |
| 测试目标 | 用户 B 携带自己的 Token 删除用户 A 的文件 ID                 |
| 前置条件 | 测试账户：QQ   token：8581887d-0812-4fd3-9c5c-d29e648db741 删除tom的文件，fildid8 |
| 测试数据 | delete：http://localhost:8080/api/file/8                     |
| 实际结果 | {<br/>    "code": 404,<br/>    "message": "文件不存在",<br/>    "data": null<br/>} |
| 验证证据 | 无法删除                                                     |
| 测试结论 | 成功                                                         |



| 测试用例 | FILE-07                                                      |
| -------- | ------------------------------------------------------------ |
| 测试目标 | 用户 A 删除自己的文件                                        |
| 前置条件 | 测试账户：QQ   token：8581887d-0812-4fd3-9c5c-d29e648db741 删除QQ的文件，fildid12 |
| 测试数据 | GET:http://localhost:8080/api/file/download/12               |
| 实际结果 | {<br/>    "code": 200,<br/>    "message": "文件删除成功",<br/>    "data": null<br/>} |
| 验证证据 | 删除文件id12                                                 |
| 测试结论 | 成功                                                         |



Token Lifecycle



| 测试用例 | TOKEN-01                                                     |
| -------- | ------------------------------------------------------------ |
| 测试目标 | 使用有效 Token 访问 /api/user/me                             |
| 前置条件 | 测试账户：tom/123456   token：54665e91-6634-4345-8167-9d2f87881a15 |
| 测试数据 | get：http://localhost:8080/api/user/me                       |
| 实际结果 | {<br/>    "code": 200,<br/>    "message": "获取成功",<br/>    "data": {<br/>        "id": 2112203056111685633,<br/>        "username": "tom",<br/>        "role": "USER"<br/>    }<br/>} |
| 验证证据 | 账户存在，访问成功                                           |
| 测试结论 | 成功                                                         |



| 测试用例 | TOKEN-02                                                     |
| -------- | ------------------------------------------------------------ |
| 测试目标 | 使用随机无效 Token 访问受保护接口                            |
| 前置条件 | 测试账户：tom/123456   token：54665e91-6634-4345-8167-9d2f87881a15w |
| 测试数据 | get：http://localhost:8080/api/user/me                       |
| 实际结果 | {<br/>    "message": "Token 无效或已失效",<br/>    "data": {},<br/>    "code": 401<br/>} |
| 验证证据 | 账户存在，访问不成功                                         |
| 测试结论 | 成功                                                         |



| 测试用例 | TOKEN-03                                                     |
| -------- | ------------------------------------------------------------ |
| 测试目标 | 使用过期 Token 访问受保护接口，访问 /api/user/me             |
| 前置条件 | 测试账户：tom/123456   token：705eb3ce-9ac0-4ea4-9435-a99a35b563d0 |
| 测试数据 | get：http://localhost:8080/api/user/me                       |
| 实际结果 | {<br/>    "message": "Token 无效或已失效",<br/>    "data": {},<br/>    "code": 401<br/>} |
| 验证证据 | 账户存在，访问不成功                                         |
| 测试结论 | 成功                                                         |



| 测试用例 | TOKEN-04                                                     |
| -------- | ------------------------------------------------------------ |
| 测试目标 | 调用注销接口后，继续使用原 Token                             |
| 前置条件 | 先post：http://localhost:8080/api/user/logout 测试账户：tom/123456   token：54665e91-6634-4345-8167-9d2f87881a15 |
| 测试数据 | get：http://localhost:8080/api/user/me  token：54665e91-6634-4345-8167-9d2f87881a15 |
| 实际结果 | {<br/>    "message": "Token 无效或已失效",<br/>    "data": {},<br/>    "code": 401<br/>} |
| 验证证据 | 账户存在，访问不成功                                         |
| 测试结论 | 成功                                                         |