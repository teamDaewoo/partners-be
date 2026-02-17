package com.dooring.domain.catalog.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 커미션 캠페인 (상품당 활성 1개, 최소 3개월)
 */
@Entity
@Table(
    name = "campaigns",
    indexes = {
        @Index(name = "campaigns_period_idx", columnList = "starts_at, ends_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 상품 (도메인 내부 참조)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * 셀러 ID (identity 도메인 참조)
     */
    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    /**
     * 건당 고정 커미션 금액 (KRW)
     */
    @Column(name = "commission_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal commissionAmount;

    /**
     * 참고용 커미션 비율 (e.g. 0.0300 = 3%)
     */
    @Column(name = "commission_rate", precision = 5, scale = 4)
    private BigDecimal commissionRate;

    /**
     * 최소 커미션 (max(3%, 3000원) 정책)
     */
    @Column(name = "min_commission", nullable = false, precision = 18, scale = 2)
    private BigDecimal minCommission;

    /**
     * 캠페인 시작 시각
     */
    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    /**
     * 캠페인 종료 시각
     */
    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    /**
     * 캠페인 활성화 여부 (셀러 수동 중단용)
     * true = 운영중, false = 셀러 수동 중단
     * 기간 만료는 starts_at/ends_at로 판단
     */
    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Campaign(Product product, Long sellerId, BigDecimal commissionAmount,
                    BigDecimal commissionRate, BigDecimal minCommission,
                    LocalDateTime startsAt, LocalDateTime endsAt) {
        validatePeriod(startsAt, endsAt);
        this.product = product;
        this.sellerId = sellerId;
        this.commissionAmount = commissionAmount;
        this.commissionRate = commissionRate;
        this.minCommission = minCommission != null ? minCommission : new BigDecimal("3000");
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 캠페인 기간 유효성 검증
     * - 종료 시각이 시작 시각보다 이후여야 함
     * - 최소 3개월(90일) 이상이어야 함
     */
    private void validatePeriod(LocalDateTime startsAt, LocalDateTime endsAt) {
        if (endsAt.isBefore(startsAt) || endsAt.isEqual(startsAt)) {
            throw new IllegalArgumentException("캠페인 종료 시각이 무족권 시작 시각보다 이후여야 함");
        }
        if (endsAt.isBefore(startsAt.plusDays(90))) {
            throw new IllegalArgumentException("Campaign 무조건 최소 3개월(90일) 이상이어야 함");
        }
    }

    /**
     * 캠페인 비활성화 (셀러 수동 중단)
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 캠페인 활성화
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * 캠페인이 현재 활성 상태이고 기간 내인지 확인
     */
    public boolean isActiveInPeriod(LocalDateTime now) {
        return this.isActive
            && !now.isBefore(this.startsAt)
            && !now.isAfter(this.endsAt);
    }
}
