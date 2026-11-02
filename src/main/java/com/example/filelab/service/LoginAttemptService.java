
package com.example.filelab.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    // 连续失败 5 次后限制登录
    private static final int MAX_ATTEMPTS = 5;

    // 限制时长：15 分钟
    private static final long LOCK_TIME_MILLIS =  15 * 60 * 1000L;

    // 普通失败记录保留时间：30 分钟
    private static final long RECORD_EXPIRE_MILLIS = 30 * 60 * 1000L;

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
    
        /**
         * 每隔 1 分钟自动清理过期记录。
         */
        @Scheduled(fixedDelay = 60000)
        public void cleanupExpiredAttempts() {

            long now = System.currentTimeMillis();

            attempts.forEach((key, record) -> {

                synchronized (record) {

                    boolean lockExpired =
                            record.lockUntil > 0
                                    && now >= record.lockUntil;

                    boolean recordExpired =
                            record.lockUntil == 0
                                    && now - record.lastFailureTime
                                    >= RECORD_EXPIRE_MILLIS;

                    if (lockExpired || recordExpired) {
                        attempts.remove(key, record);
                    }
                }
            });
        }

        /**
         * 登录失败记录。
         */
        private static class AttemptRecord {

            private int failures;

            private long lockUntil;

            private long lastFailureTime;
        }
}
