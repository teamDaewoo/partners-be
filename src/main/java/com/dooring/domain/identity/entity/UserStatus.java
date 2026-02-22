package com.dooring.domain.identity.entity;

public enum UserStatus {
    PENDING,    // 가입 후 인증 전 (현재 미사용 — 실명인증 붙일 때 활성화)
    ACTIVE,     // 정상 활성
    SUSPENDED   // 정지
}
