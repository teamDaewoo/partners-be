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

    /**
     * 로그인 식별자 (이메일)
     */
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    /**
     * 공개 닉네임
     */
    @Column(name = "nickname", nullable = false, unique = true)
    private String nickname;

    /**
     * 비밀번호 해시 (자체 인증 시 사용)
     */
    @Column(name = "password_hash", columnDefinition = "text")
    private String passwordHash;

    /**
     * 인증 제공자 (e.g. "email")
     */
    @Column(name = "auth_provider", nullable = false)
    private String authProvider;

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
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
