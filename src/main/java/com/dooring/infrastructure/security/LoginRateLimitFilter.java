package com.dooring.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;

/**
 * 로그인 엔드포인트 브루트포스 방어 필터 (IP 기반)
 * - 대상: /api/auth/seller/login, /api/auth/creator/login
 * - 정책: 1분 내 10회 초과 시 429 반환
 * - 저장소: Redis (key: ratelimit:login:{ip}, TTL: 60초)
 */
@RequiredArgsConstructor
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;

    private static final Set<String> TARGET_PATHS = Set.of(
            "/api/auth/seller/login",
            "/api/auth/creator/login"
    );
    private static final int MAX_ATTEMPTS = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final String KEY_PREFIX = "ratelimit:login:";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (!TARGET_PATHS.contains(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = KEY_PREFIX + resolveClientIp(request);
        Long rawCount = redisTemplate.opsForValue().increment(key);
        if (rawCount == null) {
            // Redis 오류 시 안전하게 통과 (fail open)
            filterChain.doFilter(request, response);
            return;
        }

        if (rawCount == 1L) {
            redisTemplate.expire(key, WINDOW);
        }

        if (rawCount > MAX_ATTEMPTS) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"success\":false,\"data\":null,\"message\":\"잠시 후 다시 시도해주세요\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /** 리버스 프록시 환경에서 실제 클라이언트 IP 추출 */
    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
