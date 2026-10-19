package com.example.filelab.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/*第一版没有过期时间
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
}*/

@Service
public class TokenService {

    // Token -> 用户 ID + 过期时间
    private final ConcurrentHashMap<String, TokenInfo> tokenStore =
            new ConcurrentHashMap<>();
    @Value("${app.auth.token-ttl-ms:7200000}")
    private long tokenTtlMillis;

    /**
     * 创建 Token，并设置过期时间
     */
    public String createToken(Long userId) {

        if (userId == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }

        if (tokenTtlMillis <= 0) {
            throw new IllegalStateException("Token 有效期必须大于 0");
        }

        // 顺便清理已经过期的 Token，降低长期积累的风险
        cleanExpiredTokens();

        String token = UUID.randomUUID().toString();

        long expiresAt = System.currentTimeMillis() + tokenTtlMillis;

        tokenStore.put(token, new TokenInfo(userId, expiresAt));

        return token;
    }

    /**
     * 获取用户 ID。
     * Token 不存在或已经过期时，返回 null。
     */
    public Long getUserId(String token) {

        if (token == null || token.isBlank()) {
            return null;
        }

        TokenInfo info = tokenStore.get(token);

        if (info == null) {
            return null;
        }

        if (System.currentTimeMillis() >= info.expiresAt) {
            // 只移除当前这条记录，避免误删其他更新后的记录
            tokenStore.remove(token, info);
            return null;
        }

        return info.userId;
    }

    /**
     * 主动注销单个 Token
     */
    public boolean invalidateToken(String token) {

        if (token == null || token.isBlank()) {
            return false;
        }

        return tokenStore.remove(token) != null;
    }

    /**
     * 撤销指定用户的所有 Token。
     * 可用于用户修改密码或管理员重置密码之后。
     */
    public void invalidateUserTokens(Long userId) {

        if (userId == null) {
            return;
        }

        tokenStore.entrySet().removeIf(
                entry -> userId.equals(entry.getValue().userId)
        );
    }

    /**
     * 清理过期 Token
     */
    private void cleanExpiredTokens() {

        long now = System.currentTimeMillis();

        tokenStore.entrySet().removeIf(
                entry -> now >= entry.getValue().expiresAt
        );
    }

    /**
     * Token 对应的数据
     */
    private static class TokenInfo {

        private final Long userId;
        private final long expiresAt;

        private TokenInfo(Long userId, long expiresAt) {
            this.userId = userId;
            this.expiresAt = expiresAt;
        }
    }
}