package com.dooring.domain.tracking.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 어필리에이트 링크 (크리에이터 × 상품 = 1:1)
 * 크리에이터가 상품에 대해 발급받는 추적 링크
 */
@Entity
@Table(
    name = "links",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "links_creator_product_uniq",
            columnNames = {"creator_id", "product_id"}
        )
    },
    indexes = {
        @Index(name = "links_short_code_idx", columnList = "short_code")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Link {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 크리에이터 ID (identity 도메인 참조)
     */
    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    /**
     * 상품 ID (catalog 도메인 참조)
     */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /**
     * 리다이렉트용 고유 단축 코드 (e.g. "aB3kX9")
     */
    @Column(name = "short_code", nullable = false, unique = true)
    private String shortCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Link(Long creatorId, Long productId, String shortCode) {
        this.creatorId = creatorId;
        this.productId = productId;
        this.shortCode = shortCode;
        this.createdAt = LocalDateTime.now();
    }
}
