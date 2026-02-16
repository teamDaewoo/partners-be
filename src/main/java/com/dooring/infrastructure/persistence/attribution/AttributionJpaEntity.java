package com.dooring.infrastructure.persistence.attribution;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Attribution JPA Entity
 * infrastructure 레이어에만 존재 (도메인 모델과 분리)
 */
@Entity
@Table(
    name = "attributions",
    indexes = {
        @Index(name = "attributions_order_id_idx", columnList = "order_id")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttributionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "click_id", nullable = false)
    private Long clickId;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "attributed_at", nullable = false, updatable = false)
    private LocalDateTime attributedAt;

    /**
     * CommissionLedger와 1:1 관계 (양방향, mappedBy)
     * CascadeType.ALL: Attribution 저장 시 CommissionLedger도 함께 저장
     * orphanRemoval: Attribution 삭제 시 CommissionLedger도 삭제
     *
     * CommissionLedgerJpaEntity가 FK(attribution_id)를 소유
     */
    @OneToOne(mappedBy = "attribution", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private CommissionLedgerJpaEntity commissionLedger;

    /**
     * JPA Entity 생성자 (Adapter에서만 사용)
     */
    public AttributionJpaEntity(Long orderId, Long clickId, Long campaignId,
                                LocalDateTime attributedAt,
                                CommissionLedgerJpaEntity commissionLedger) {
        this.orderId = orderId;
        this.clickId = clickId;
        this.campaignId = campaignId;
        this.attributedAt = attributedAt;
        setCommissionLedger(commissionLedger);
    }

    /**
     * 양방향 관계 설정 헬퍼
     */
    private void setCommissionLedger(CommissionLedgerJpaEntity commissionLedger) {
        this.commissionLedger = commissionLedger;
        if (commissionLedger != null) {
            commissionLedger.setAttribution(this);
        }
    }
}
