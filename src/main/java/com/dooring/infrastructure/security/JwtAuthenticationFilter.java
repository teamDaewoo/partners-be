package com.dooring.infrastructure.security;

import com.dooring.domain.identity.entity.UserStatus;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 인증 필터 — 요청마다 1회 실행
 *
 * 처리 흐름:
 * 1. Authorization: Bearer {AT} 헤더 추출
 * 2. AT 서명 검증 + 만료 확인
 * 3. Claims에서 userId, type(seller|creator), status 파싱
 * 4. type에 따라 SellerPrincipal / CreatorPrincipal 생성
 * 5. status == ACTIVE 인 경우에만 SecurityContext에 Authentication 설정
 * 6. 토큰 없음 / 유효하지 않음 → 그냥 통과 (SecurityContext 비워둠 → 이후 인가 실패)
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.isTokenValid(token)) {
            Claims claims = jwtTokenProvider.parseToken(token);

            Long userId     = Long.parseLong(claims.getSubject());
            String type     = claims.get("type", String.class);
            String statusStr = claims.get("status", String.class);

            UserStatus status = UserStatus.valueOf(statusStr.toUpperCase());

            UserDetails principal = "seller".equals(type)
                    ? new SellerPrincipal(userId, status)
                    : new CreatorPrincipal(userId, status);

            // SUSPENDED 계정은 SecurityContext에 넣지 않음 → 401 처리
            if (principal.isEnabled()) {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
