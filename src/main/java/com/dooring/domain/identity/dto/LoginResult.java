package com.dooring.domain.identity.dto;

/**
 * 로그인 / 토큰 갱신 내부 전달 객체.
 * Controller가 accessToken은 body로, refreshToken은 httpOnly Cookie로 분리하여 응답함.
 */
public record LoginResult(String accessToken, String refreshToken) {}
