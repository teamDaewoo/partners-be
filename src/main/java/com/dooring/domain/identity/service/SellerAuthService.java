package com.dooring.domain.identity.service;

import com.dooring.common.exception.BusinessException;
import com.dooring.common.exception.ErrorCode;
import com.dooring.domain.identity.dto.LoginRequest;
import com.dooring.domain.identity.dto.LoginResult;
import com.dooring.domain.identity.dto.SellerSignupRequest;
import com.dooring.domain.identity.dto.SignupResponse;
import com.dooring.domain.identity.entity.Seller;
import com.dooring.domain.identity.entity.UserStatus;
import com.dooring.domain.identity.entity.UserType;
import com.dooring.domain.identity.repository.SellerRepository;
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
public class SellerAuthService {

    private final SellerRepository sellerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;

    // ----------------------------------------------------------------
    // 회원가입
    // ----------------------------------------------------------------

    @Transactional
    public SignupResponse signup(SellerSignupRequest request) {
        if (sellerRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        Seller seller = Seller.builder()
                .email(request.email())
                .name(request.name())
                .passwordHash(passwordEncoder.encode(request.password()))
                .authProvider("email")
                .build();

        sellerRepository.save(seller);
        return SignupResponse.ofSeller(seller);
    }

    // ----------------------------------------------------------------
    // 로그인
    // ----------------------------------------------------------------

    @Transactional
    public LoginResult login(LoginRequest request) {
        Seller seller = sellerRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PASSWORD));

        if (!passwordEncoder.matches(request.password(), seller.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        if (seller.getStatus() == UserStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
        }

        return issueTokens(seller.getId(), seller.getStatus());
    }

    // ----------------------------------------------------------------
    // 로그아웃
    // ----------------------------------------------------------------

    @Transactional
    public void logout(Long userId) {
        refreshTokenStore.delete(UserType.SELLER, userId);
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
        refreshTokenStore.validate(UserType.SELLER, userId, tokenFamily, refreshToken);

        Seller seller = sellerRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        if (seller.getStatus() == UserStatus.SUSPENDED) {
            refreshTokenStore.delete(UserType.SELLER, userId);
            throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
        }

        return issueTokens(seller.getId(), seller.getStatus());
    }

    // ----------------------------------------------------------------
    // 내부 유틸
    // ----------------------------------------------------------------

    /** AT + RT 발급 및 Redis 저장 */
    private LoginResult issueTokens(Long userId, UserStatus status) {
        String tokenFamily = UUID.randomUUID().toString();
        String at = jwtTokenProvider.generateAccessToken(userId, UserType.SELLER, status);
        String rt = jwtTokenProvider.generateRefreshToken(userId, UserType.SELLER, tokenFamily);

        refreshTokenStore.save(
                UserType.SELLER, userId, tokenFamily, rt,
                jwtTokenProvider.getRefreshTokenExpirationMs());

        return new LoginResult(at, rt);
    }
}
