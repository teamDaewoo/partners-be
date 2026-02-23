package com.dooring.domain.identity.dto;

/**
 * 액세스 토큰 응답 DTO.
 * RT는 httpOnly Cookie로 내려가므로 body에 포함하지 않음.
 */
public record TokenResponse(String accessToken) {}
