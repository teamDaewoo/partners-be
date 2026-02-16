package com.dooring.domain.identity.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 연동 쇼핑몰 플랫폼 (카페24, 아임웹 등)
 */
@Entity
@Table(name = "platforms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Platform {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 플랫폼 코드 (e.g. "cafe24", "imweb")
     */
    @Column(name = "code", nullable = false, unique = true)
    private String code;

    /**
     * 플랫폼 표시명
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * API base URL (연동용)
     */
    @Column(name = "base_api_url", columnDefinition = "text")
    private String baseApiUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Platform(String code, String name, String baseApiUrl) {
        this.code = code;
        this.name = name;
        this.baseApiUrl = baseApiUrl;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
