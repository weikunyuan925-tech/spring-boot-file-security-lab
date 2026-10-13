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

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws IOException {

        String token = request.getHeader("token");
        Long userId = tokenService.getUserId(token);

        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json");

            response.getWriter().write(
                    objectMapper.writeValueAsString(
                            Map.of(
                                    "code", 401,
                                    "message", "Token 无效或已失效",
                                    "data", Map.of()
                            )
                    )
            );

            return false;
        }

        // 将认证后的用户 ID 交给后续 Controller
        request.setAttribute("userId", userId);

        return true;
    }
}