package com.example.filelab.service;

import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenService {

    // token -> userId
    private final ConcurrentHashMap<String, Long> tokenStore =
            new ConcurrentHashMap<>();//ConcurrentHashMap 可以理解为适合多线程同时读写的 Map。

    public String createToken(Long userId) {
        String token = UUID.randomUUID().toString();
        tokenStore.put(token, userId);
        return token;
    }

    public Long getUserId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return tokenStore.get(token);//检查这个 Token 是否由当前服务器签发并且仍然存在的关键步骤。
    }

    public void removeToken(String token) {
        if (token != null) {
            tokenStore.remove(token);
        }
    }
}