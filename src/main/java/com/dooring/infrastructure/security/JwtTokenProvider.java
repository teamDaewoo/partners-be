package com.dooring.infrastructure.security;

import com.dooring.domain.identity.entity.UserStatus;
import com.dooring.domain.identity.entity.UserType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT 액세스 토큰 / 리프레시 토큰 생성 및 검증
 *
 * AT payload: sub(userId), type(seller|creator), status(active|...), iat, exp
 * RT payload: sub(userId), type(seller|creator), family(tokenFamily UUID), iat, exp
 */
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;

    // ----------------------------------------------------------------
    // 토큰 생성
    // ----------------------------------------------------------------

    public String generateAccessToken(Long userId, UserType userType, UserStatus status) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", userType.name().toLowerCase())
                .claim("status", status.name().toLowerCase())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpirationMs))
                .signWith(signingKey())
                .compact();
    }

    public String generateRefreshToken(Long userId, UserType userType, String tokenFamily) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", userType.name().toLowerCase())
                .claim("family", tokenFamily)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshTokenExpirationMs))
                .signWith(signingKey())
                .compact();
    }

    // ----------------------------------------------------------------
    // 토큰 파싱 / 검증
    // ----------------------------------------------------------------

    /**
     * 토큰 서명 검증 + 만료 확인 후 Claims 반환.
     * 유효하지 않으면 JwtException 던짐 — 호출부에서 처리.
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 토큰이 유효한지 boolean 반환 (필터에서 사용).
     */
    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // ----------------------------------------------------------------
    // Getter (서비스 레이어에서 TTL 계산용)
    // ----------------------------------------------------------------

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    // ----------------------------------------------------------------
    // 내부 유틸
    // ----------------------------------------------------------------

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}
