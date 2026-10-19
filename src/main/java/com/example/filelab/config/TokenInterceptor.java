package com.example.filelab.config;

import com.example.filelab.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

@Component
public class TokenInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;
    private final ObjectMapper objectMapper;

    public TokenInterceptor(
            TokenService tokenService,
            ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
    }

    private void writeUnauthorizedResponse(
            HttpServletResponse response,
            String message) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> result = Map.of(
                "code", 401,
                "message", message,
                "data", Map.of()
        );

        objectMapper.writeValue(response.getWriter(), result);
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws IOException {

        // 1. 获取 Authorization 请求头
        String authHeader = request.getHeader("Authorization");

        // 2. 检查请求头是否存在，以及格式是否正确
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeUnauthorizedResponse(response, "请提供有效的 Token");
            return false;
        }

        // 3. 提取 Token
        String token = authHeader.substring(7).trim();

        if (token.isEmpty()) {
            writeUnauthorizedResponse(response, "Token 不能为空");
            return false;
        }

        // 4. 验证 Token，获取用户 ID
        Long userId = tokenService.getUserId(token);

        if (userId == null) {
            writeUnauthorizedResponse(response, "Token 无效或已失效");
            return false;
        }

        // 5. 将认证后的用户 ID 交给 Controller
        request.setAttribute("userId", userId);

        // 6. 保存当前 Token，供注销接口使用
        request.setAttribute("token", token);

        return true;

    }
}