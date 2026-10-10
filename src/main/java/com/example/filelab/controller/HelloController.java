package com.example.filelab.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController// 声明控制器  告诉Spring：这个类负责处理Web请求，方法返回的字符串等内容可以直接作为HTTP响应返回。
//请确认启动类位于 com.example.filelab 包中，而控制器位于它的子包 com.example.filelab.controller。这样 Spring Boot 默认的组件扫描就能找到控制器。
public class HelloController {

    @GetMapping("/hello")//配置路由  将HTTP GET 请求映射到 /hello 这个路径。
    public String hello() {
        return "Hello Java Security Lab";
    }
}
