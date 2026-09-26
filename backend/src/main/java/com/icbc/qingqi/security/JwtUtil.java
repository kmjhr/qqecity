package com.icbc.qingqi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * JWT 工具类
 * <p>
 * 职责：生成 Token、解析 Token、验证 Token、Token 黑名单
 * 令牌策略：
 * - access_token：有效期 2 小时，用于接口访问
 * - refresh_token：有效期 7 天，用于刷新 access_token
 * - 登出/修改密码时，将 access_token 加入 Redis 黑名单
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-ttl}")
    private Long accessTokenTtl;

    @Value("${jwt.refresh-token-ttl}")
    private Long refreshTokenTtl;

    @Value("${jwt.token-prefix}")
    private String tokenPrefix;

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type"; // access / refresh

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final RedisTemplate<String, Object> redisTemplate;

    public JwtUtil(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 生成访问令牌
     */
    public String generateAccessToken(Long userId, String username, String role) {
        return buildToken(userId, username, role, "access", accessTokenTtl);
    }

    /**
     * 生成刷新令牌
     */
    public String generateRefreshToken(Long userId, String username, String role) {
        return buildToken(userId, username, role, "refresh", refreshTokenTtl);
    }

    private String buildToken(Long userId, String username, String role, String type, Long ttlSeconds) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + ttlSeconds * 1000);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(expire)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析 Token，返回 Claims（不做有效性检查，调用方自行处理异常）
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Bearer 头中提取纯 Token
     */
    public String extractToken(String header) {
        if (header != null && header.startsWith(tokenPrefix)) {
            return header.substring(tokenPrefix.length());
        }
        return header;
    }

    /**
     * 校验 Token 是否有效（未过期 + 不在黑名单中）
     *
     * @return null 表示有效，否则返回错误标识
     */
    public String validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            // 检查是否在黑名单
            Boolean blacklisted = redisTemplate.hasKey(BLACKLIST_PREFIX + token);
            if (Boolean.TRUE.equals(blacklisted)) {
                return "TOKEN_BLACKLISTED";
            }
            // 检查类型是否为 access
            String type = claims.get(CLAIM_TYPE, String.class);
            if (!"access".equals(type)) {
                return "TOKEN_TYPE_INVALID";
            }
            return null; // 有效
        } catch (ExpiredJwtException e) {
            return "TOKEN_EXPIRED";
        } catch (Exception e) {
            log.warn("Token 解析失败：{}", e.getMessage());
            return "TOKEN_INVALID";
        }
    }

    /**
     * 将 Token 加入黑名单（登出时调用）
     */
    public void invalidateToken(String token) {
        try {
            Claims claims = parseToken(token);
            long ttl = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (ttl > 0) {
                redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, 1, ttl, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            log.warn("Token 失效失败（可能已过期）：{}", e.getMessage());
        }
    }

    /**
     * 从 Token 中获取用户 ID
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return claims.get(CLAIM_USER_ID, Long.class);
    }

    /**
     * 从 Token 中获取用户名
     */
    public String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims.get(CLAIM_USERNAME, String.class);
    }

    /**
     * 从 Token 中获取角色
     */
    public String getRole(String token) {
        Claims claims = parseToken(token);
        return claims.get(CLAIM_ROLE, String.class);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
