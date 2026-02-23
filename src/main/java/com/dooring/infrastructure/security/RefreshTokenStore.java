package com.dooring.infrastructure.security;

import com.dooring.common.exception.BusinessException;
import com.dooring.common.exception.ErrorCode;
import com.dooring.domain.identity.entity.UserType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 Refresh Token 저장소
 * key   : refresh:{userType}:{userId}   (e.g. refresh:seller:42)
 * value : {tokenFamily}:{sha256(refreshToken)}
 * RT Rotation 정책:
 *   - 갱신마다 새 tokenFamily + 새 RT 발급 → save() 재호출
 *   - tokenFamily 또는 RT 해시 불일치 → 탈취 간주 → 키 삭제(강제 로그아웃)
 */
@Component
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;

    public RefreshTokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ----------------------------------------------------------------
    // public API
    // ----------------------------------------------------------------

    /**
     * RT 저장 — 로그인 / 토큰 갱신 시 호출.
     * 기존 키가 있으면 덮어씀 (싱글 세션 정책).
     */
    public void save(UserType userType, Long userId,
                     String tokenFamily, String refreshToken, long expirationMs) {
        String key   = buildKey(userType, userId);
        String value = tokenFamily + ":" + sha256(refreshToken);
        redisTemplate.opsForValue().set(key, value, expirationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * RT 검증 — 토큰 갱신 요청 시 호출.
     * 1) 키 없음          → 만료 또는 로그아웃  → REFRESH_TOKEN_NOT_FOUND
     * 2) tokenFamily 불일치 → 탈취 감지         → 키 삭제 후 REFRESH_TOKEN_STOLEN
     * 3) RT 해시 불일치    → 이미 사용된 RT      → 키 삭제 후 REFRESH_TOKEN_STOLEN
     */
    public void validate(UserType userType, Long userId,
                         String tokenFamily, String refreshToken) {
        String key    = buildKey(userType, userId);
        String stored = redisTemplate.opsForValue().get(key);

        if (stored == null) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        String[] parts       = stored.split(":", 2);
        String storedFamily  = parts[0];
        String storedHash    = parts[1];

        if (!storedFamily.equals(tokenFamily) || !storedHash.equals(sha256(refreshToken))) {
            redisTemplate.delete(key);   // 강제 로그아웃
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_STOLEN);
        }
    }

    /**
     * RT 삭제 — 로그아웃 시 호출.
     */
    public void delete(UserType userType, Long userId) {
        redisTemplate.delete(buildKey(userType, userId));
    }

    // ----------------------------------------------------------------
    // 내부 유틸
    // ----------------------------------------------------------------

    private String buildKey(UserType userType, Long userId) {
        return KEY_PREFIX + userType.name().toLowerCase() + ":" + userId;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
