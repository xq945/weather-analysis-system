package com.weather.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP 级别登录防暴力破解：5 次失败后锁定 5 分钟
 *
 * 使用 ConcurrentHashMap 存储每个 IP 的尝试记录，无外部依赖。
 * 锁定超时后自动清除记录，允许重新尝试。
 */
@Component
public class LoginRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimiter.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(5);

    private final ConcurrentHashMap<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

    /**
     * 检查当前 IP 是否被锁定
     *
     * @throws IllegalArgumentException 如果 IP 已被临时封锁
     */
    public void check(HttpServletRequest request) {
        String ip = getClientIp(request);
        AttemptRecord record = attempts.get(ip);
        if (record != null && record.count >= MAX_ATTEMPTS) {
            Duration elapsed = Duration.between(record.lastAttempt, Instant.now());
            if (elapsed.compareTo(LOCK_DURATION) < 0) {
                long remain = LOCK_DURATION.toSeconds() - elapsed.toSeconds();
                throw new IllegalArgumentException("登录尝试过于频繁，请 " + remain + " 秒后重试");
            }
            // 锁定超时，清除记录
            attempts.remove(ip);
        }
    }

    /**
     * 记录一次登录失败
     *
     * 如果该 IP 已达最大尝试次数，输出警告日志。
     */
    public void recordFailure(HttpServletRequest request) {
        String ip = getClientIp(request);
        attempts.compute(ip, (key, record) -> {
            // 无记录或上次锁定已超时，重置计数
            if (record == null || Duration.between(record.lastAttempt, Instant.now()).compareTo(LOCK_DURATION) >= 0) {
                return new AttemptRecord(1, Instant.now());
            }
            record.count++;
            record.lastAttempt = Instant.now();
            if (record.count >= MAX_ATTEMPTS) {
                log.warn("IP 已被临时封锁: {}", key);
            }
            return record;
        });
    }

    /** 登录成功，清除该 IP 的失败记录 */
    public void recordSuccess(HttpServletRequest request) {
        attempts.remove(getClientIp(request));
    }

    /**
     * 获取客户端真实 IP
     *
     * 优先取 X-Forwarded-For，其次 X-Real-IP，最后取 remoteAddr。
     */
    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank() && !"unknown".equalsIgnoreCase(xff)) {
            return xff.split(",")[0].trim();
        }
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank() && !"unknown".equalsIgnoreCase(xri)) {
            return xri.trim();
        }
        return request.getRemoteAddr();
    }

    private static class AttemptRecord {
        int count;
        Instant lastAttempt;

        AttemptRecord(int count, Instant lastAttempt) {
            this.count = count;
            this.lastAttempt = lastAttempt;
        }
    }
}
