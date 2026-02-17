package com.dooring.domain.catalog.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 스토어 상품 (플랫폼 API 동기화 기준 단위)
 */
@Entity
@Table(
    name = "products",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "products_store_ext_uniq",
            columnNames = {"store_id", "external_product_id"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 스토어 (도메인 내부 참조)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    /**
     * 플랫폼 API가 제공하는 상품 ID
     */
    @Column(name = "external_product_id", nullable = false)
    private String externalProductId;

    /**
     * 상품명
     */
    @Column(name = "name", columnDefinition = "text")
    private String name;

    /**
     * 대표 이미지 (큐레이션 페이지용)
     */
    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    /**
     * 상품 상세 페이지 URL (리다이렉트 대상)
     */
    @Column(name = "product_url", columnDefinition = "text")
    private String productUrl;

    /**
     * 현재 판매가 (동기화)
     */
    @Column(name = "price", precision = 18, scale = 2)
    private BigDecimal price;

    /**
     * 상품 활성화 여부
     */
    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    /**
     * 마지막 API 동기화 시점
     */
    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Product(Store store, String externalProductId, String name,
                   String imageUrl, String productUrl, BigDecimal price) {
        this.store = store;
        this.externalProductId = externalProductId;
        this.name = name;
        this.imageUrl = imageUrl;
        this.productUrl = productUrl;
        this.price = price;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 상품 정보 동기화
     */
    public void sync(String name, String imageUrl, String productUrl, BigDecimal price) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.productUrl = productUrl;
        this.price = price;
        this.lastSyncedAt = LocalDateTime.now();
    }

    /**
     * 상품 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 상품 활성화
     */
    public void activate() {
        this.isActive = true;
    }
}
