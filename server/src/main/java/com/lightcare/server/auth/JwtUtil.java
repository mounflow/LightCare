package com.lightcare.server.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 签发 / 解析工具（jjwt 0.12.5）。
 *
 * 配置：
 *   app.jwt.secret = 至少 32 字节字符串（HMAC-SHA256 要求）
 *   app.jwt.ttl-hours = token 有效期（默认 168 = 7 天）
 *
 * 失败一律抛 RuntimeException（Filter 阶段 catch 后转 401 JSON）。
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long ttlMillis;

    public JwtUtil(@Value("${app.jwt.secret:}") String secret,
                   @Value("${app.jwt.ttl-hours:168}") long ttlHours) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                "app.jwt.secret 至少 32 字节；当前未配置或太短。请在 application.properties 设置 app.jwt.secret=<64+ char>");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlMillis = Duration.ofHours(ttlHours).toMillis();
    }

    /** 签发 token。claims 里塞 userId（Long）和 phone（String）。 */
    public String issue(long userId, String phone) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(String.valueOf(userId))
            .claim("uid", userId)
            .claim("phone", phone)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(ttlMillis)))
            .signWith(key)
            .compact();
    }

    /**
     * 解析 token → userId。失败抛异常（filter 层 catch 后转 401）。
     * @throws io.jsonwebtoken.JwtException 各种解析失败（过期 / 签名错 / 格式坏）
     */
    public long parse(String token) {
        var claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        // 优先用 uid claim；fallback 用 subject
        Object uid = claims.get("uid");
        if (uid instanceof Number n) return n.longValue();
        try { return Long.parseLong(claims.getSubject()); }
        catch (Exception e) {
            throw new io.jsonwebtoken.JwtException("token 缺少有效 uid/subject");
        }
    }
}