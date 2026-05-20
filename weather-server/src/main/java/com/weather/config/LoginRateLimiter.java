package com.weather.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimiter.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(5);

    private final ConcurrentHashMap<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

    public void check(HttpServletRequest request) {
        String ip = getClientIp(request);
        AttemptRecord record = attempts.get(ip);
        if (record != null && record.count >= MAX_ATTEMPTS) {
            Duration elapsed = Duration.between(record.lastAttempt, Instant.now());
            if (elapsed.compareTo(LOCK_DURATION) < 0) {
                long remain = LOCK_DURATION.toSeconds() - elapsed.toSeconds();
                throw new IllegalArgumentException("登录尝试过于频繁，请 " + remain + " 秒后重试");
            }
            attempts.remove(ip);
        }
    }

    public void recordFailure(HttpServletRequest request) {
        String ip = getClientIp(request);
        attempts.compute(ip, (key, record) -> {
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

    public void recordSuccess(HttpServletRequest request) {
        attempts.remove(getClientIp(request));
    }

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
