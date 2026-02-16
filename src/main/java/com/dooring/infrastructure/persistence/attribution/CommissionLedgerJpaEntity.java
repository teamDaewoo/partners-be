package com.dooring.infrastructure.persistence.attribution;

import com.dooring.domain.attribution.aggregate.CommissionStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CommissionLedger JPA Entity
 * infrastructure 레이어에만 존재 (도메인 모델과 분리)
 */
@Entity
@Table(name = "commission_ledgers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommissionLedgerJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Attribution과 1:1 관계 (양방향, FK 소유)
     * commission_ledgers.attribution_id가 FK
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribution_id", nullable = false)
    private AttributionJpaEntity attribution;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CommissionStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /**
     * JPA Entity 생성자 (Adapter에서만 사용)
     */
    public CommissionLedgerJpaEntity(Long campaignId, Long creatorId, Long sellerId,
                                     BigDecimal amount, CommissionStatus status,
                                     LocalDateTime createdAt, LocalDateTime updatedAt,
                                     LocalDateTime confirmedAt, LocalDateTime paidAt) {
        this.campaignId = campaignId;
        this.creatorId = creatorId;
        this.sellerId = sellerId;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.confirmedAt = confirmedAt;
        this.paidAt = paidAt;
    }

    /**
     * 상태 업데이트 (도메인 로직 실행 후 동기화용)
     */
    public void updateStatus(CommissionStatus status, LocalDateTime confirmedAt, LocalDateTime paidAt) {
        this.status = status;
        this.confirmedAt = confirmedAt;
        this.paidAt = paidAt;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 양방향 관계 설정 (Adapter에서만 사용)
     */
    void setAttribution(AttributionJpaEntity attribution) {
        this.attribution = attribution;
    }
}
