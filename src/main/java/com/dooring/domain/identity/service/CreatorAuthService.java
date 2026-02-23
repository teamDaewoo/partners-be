package com.dooring.domain.identity.service;

import com.dooring.common.exception.BusinessException;
import com.dooring.common.exception.ErrorCode;
import com.dooring.domain.identity.dto.CreatorSignupRequest;
import com.dooring.domain.identity.dto.LoginRequest;
import com.dooring.domain.identity.dto.LoginResult;
import com.dooring.domain.identity.dto.SignupResponse;
import com.dooring.domain.identity.entity.Creator;
import com.dooring.domain.identity.entity.UserStatus;
import com.dooring.domain.identity.entity.UserType;
import com.dooring.domain.identity.repository.CreatorRepository;
import com.dooring.infrastructure.security.JwtTokenProvider;
import com.dooring.infrastructure.security.RefreshTokenStore;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreatorAuthService {

    private final CreatorRepository creatorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;

    // ----------------------------------------------------------------
    // 회원가입
    // ----------------------------------------------------------------

    @Transactional
    public SignupResponse signup(CreatorSignupRequest request) {
        if (creatorRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (creatorRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        Creator creator = Creator.builder()
                .email(request.email())
                .nickname(request.nickname())
                .passwordHash(passwordEncoder.encode(request.password()))
                .authProvider("email")
                .build();

        creatorRepository.save(creator);
        return SignupResponse.ofCreator(creator);
    }

    // ----------------------------------------------------------------
    // 로그인
    // ----------------------------------------------------------------

    @Transactional
    public LoginResult login(LoginRequest request) {
        Creator creator = creatorRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PASSWORD));

        if (!passwordEncoder.matches(request.password(), creator.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        if (creator.getStatus() == UserStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
        }

        return issueTokens(creator.getId(), creator.getStatus());
    }

    // ----------------------------------------------------------------
    // 로그아웃
    // ----------------------------------------------------------------

    @Transactional
    public void logout(Long userId) {
        refreshTokenStore.delete(UserType.CREATOR, userId);
    }

    // ----------------------------------------------------------------
    // 토큰 갱신
    // ----------------------------------------------------------------

    @Transactional
    public LoginResult refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        Claims claims;
        try {
            claims = jwtTokenProvider.parseToken(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        Long userId = Long.parseLong(claims.getSubject());
        String tokenFamily = claims.get("family", String.class);

        // Redis 검증 (탈취 감지 포함)
        refreshTokenStore.validate(UserType.CREATOR, userId, tokenFamily, refreshToken);

        Creator creator = creatorRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREATOR_NOT_FOUND));

        if (creator.getStatus() == UserStatus.SUSPENDED) {
            refreshTokenStore.delete(UserType.CREATOR, userId);
            throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
        }

        return issueTokens(creator.getId(), creator.getStatus());
    }

    // ----------------------------------------------------------------
    // 내부 유틸
    // ----------------------------------------------------------------

    /** AT + RT 발급 및 Redis 저장 */
    private LoginResult issueTokens(Long userId, UserStatus status) {
        String tokenFamily = UUID.randomUUID().toString();
        String at = jwtTokenProvider.generateAccessToken(userId, UserType.CREATOR, status);
        String rt = jwtTokenProvider.generateRefreshToken(userId, UserType.CREATOR, tokenFamily);

        refreshTokenStore.save(
                UserType.CREATOR, userId, tokenFamily, rt,
                jwtTokenProvider.getRefreshTokenExpirationMs());

        return new LoginResult(at, rt);
    }
}
