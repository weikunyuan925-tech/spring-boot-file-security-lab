
package com.example.filelab.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    // 连续失败 5 次后限制登录
    private static final int MAX_ATTEMPTS = 5;

    // 限制时长：15 分钟
    private static final long LOCK_TIME_MILLIS = 1 * 60 * 1000L;

    private final ConcurrentHashMap<String, AttemptRecord> attempts =
            new ConcurrentHashMap<>();

    /**
     * 组合用户名和客户端 IP，形成限流键。
     */
    private String buildKey(String username, String ip) {
        return username.trim().toLowerCase() + "|" + ip;
    }

    /**
     * 检查当前组合是否处于限制期。
     */
    public boolean isBlocked(String username, String ip) {
        String key = buildKey(username, ip);
        AttemptRecord record = attempts.get(key);

        if (record == null) {
            return false;
        }

        synchronized (record) {
            long now = System.currentTimeMillis();

            // 限制期结束，清除记录
            if (record.lockUntil > 0 && now >= record.lockUntil) {
                attempts.remove(key, record);
                return false;
            }

            return record.lockUntil > now;
        }
    }

    /**
     * 记录一次失败的登录尝试。
     */
    public boolean recordFailure(String username, String ip) {
        String key = buildKey(username, ip);

        AttemptRecord record = attempts.computeIfAbsent(
                key, ignored -> new AttemptRecord()
        );

        synchronized (record) {
            long now = System.currentTimeMillis();

            // 如果此前的限制期已经结束，重新开始统计
            if (record.lockUntil > 0 && now >= record.lockUntil) {
                record.failures = 0;
                record.lockUntil = 0;
            }

            record.failures++;

            if (record.failures >= MAX_ATTEMPTS) {
                record.lockUntil = now + LOCK_TIME_MILLIS;
                return true;
            }

            return false;
        }
    }

    /**
     * 登录成功后清除失败记录。
     */
    public void recordSuccess(String username, String ip) {
        attempts.remove(buildKey(username, ip));
    }

    private static class AttemptRecord {
        private int failures;
        private long lockUntil;
    }
}
