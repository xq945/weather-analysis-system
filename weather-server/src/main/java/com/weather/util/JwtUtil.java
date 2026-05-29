package com.weather.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT 工具类：HS512 密钥自动生成，支持 Token 签发与解析
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expiration;

    /**
     * 构造时自动生成 HS512 密钥
     *
     * @param expiration Token 过期时间（毫秒），从配置 jwt.expiration 读取
     */
    public JwtUtil(@Value("${jwt.expiration}") long expiration) {
        this.key = Jwts.SIG.HS512.key().build();
        this.expiration = expiration;
    }

    /**
     * 生成 JWT Token
     *
     * @param userId   用户 ID（存为 subject）
     * @param username 用户名（存入 claims）
     * @return JWT Token 字符串
     */
    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * 从 Token 中解析用户 ID
     *
     * @param token JWT Token
     * @return 用户 ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.parseLong(claims.getSubject());
    }
}
