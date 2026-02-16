package com.dooring.domain.identity.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 셀러 (쇼핑몰 운영자, SaaS 결제 주체)
 */
@Entity
@Table(name = "sellers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 로그인 식별자 (이메일)
     */
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    /**
     * 사업자명 또는 대표자명
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * 비밀번호 해시 (자체 인증 시 사용)
     */
    @Column(name = "password_hash", columnDefinition = "text")
    private String passwordHash;

    /**
     * 인증 제공자 (e.g. "email", "cafe24_oauth", "imweb_oauth")
     */
    @Column(name = "auth_provider", nullable = false)
    private String authProvider;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Seller(String email, String name, String passwordHash, String authProvider) {
        this.email = email;
        this.name = name;
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
