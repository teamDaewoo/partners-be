package com.dooring.domain.catalog.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 셀러의 개별 스토어 (플랫폼당 고유)
 */
@Entity
@Table(
    name = "stores",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "stores_platform_ext_uniq",
            columnNames = {"platform_id", "external_store_id"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 셀러 ID (identity 도메인 참조)
     */
    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    /**
     * 플랫폼 ID (identity 도메인 참조)
     */
    @Column(name = "platform_id", nullable = false)
    private Long platformId;

    /**
     * 플랫폼에서 부여한 스토어 ID
     */
    @Column(name = "external_store_id", nullable = false)
    private String externalStoreId;

    /**
     * 스토어 표시명
     */
    @Column(name = "name", columnDefinition = "text")
    private String name;

    /**
     * 플랫폼 API 토큰 (암호화 저장 권장)
     */
    @Column(name = "access_token", columnDefinition = "text")
    private String accessToken;

    /**
     * 스토어 활성화 여부
     */
    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Store(Long sellerId, Long platformId, String externalStoreId,
                 String name, String accessToken) {
        this.sellerId = sellerId;
        this.platformId = platformId;
        this.externalStoreId = externalStoreId;
        this.name = name;
        this.accessToken = accessToken;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 스토어 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 스토어 활성화
     */
    public void activate() {
        this.isActive = true;
    }
}
