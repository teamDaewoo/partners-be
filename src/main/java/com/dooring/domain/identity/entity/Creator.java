package com.dooring.domain.identity.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 어필리에이트 크리에이터 (오픈 마켓 참여자)
 */
@Entity
@Table(name = "creators")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Creator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 로그인 식별자 (이메일) */
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    /** 공개 닉네임 */
    @Column(name = "nickname", nullable = false, unique = true)
    private String nickname;

    /** 비밀번호 해시 (자체 인증 시 사용) */
    @Column(name = "password_hash", columnDefinition = "text")
    private String passwordHash;

    /** 인증 제공자 (e.g. "email") */
    @Column(name = "auth_provider", nullable = false)
    private String authProvider;

    /** 회원 상태 (PENDING → 인증 전 / ACTIVE → 정상 / SUSPENDED → 정지) */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "user_status_enum")
    private UserStatus status;

    /** soft delete 시각 (null 이면 정상 계정) */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Creator(String email, String nickname, String passwordHash, String authProvider) {
        this.email = email;
        this.nickname = nickname;
        this.passwordHash = passwordHash;
        this.authProvider = authProvider != null ? authProvider : "email";
        this.status = UserStatus.ACTIVE; // 1차: 실명인증 없이 즉시 활성
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public void suspend() {
        this.status = UserStatus.SUSPENDED;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.status = UserStatus.SUSPENDED;
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
