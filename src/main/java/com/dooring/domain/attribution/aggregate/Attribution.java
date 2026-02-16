package com.dooring.domain.attribution.aggregate;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 구매 귀속 Aggregate Root (last-click, 주문당 1건)
 * CommissionLedger를 소유하며 함께 생성/관리
 */
@Getter
public class Attribution {

    private Long id;
    private final Long orderId;
    private final Long clickId;
    private final Long campaignId;
    private final LocalDateTime attributedAt;
    private final CommissionLedger commissionLedger;

    /**
     * 귀속 생성 (CommissionLedger도 함께 생성)
     *
     * @param orderId 주문 ID
     * @param clickId 클릭 ID
     * @param campaignId 캠페인 ID
     * @param creatorId 크리에이터 ID
     * @param sellerId 셀러 ID
     * @param commissionAmount 커미션 금액
     */
    public Attribution(Long orderId, Long clickId, Long campaignId,
                       Long creatorId, Long sellerId, BigDecimal commissionAmount) {
        validateConstructorParams(orderId, clickId, campaignId, creatorId, sellerId, commissionAmount);

        this.orderId = orderId;
        this.clickId = clickId;
        this.campaignId = campaignId;
        this.attributedAt = LocalDateTime.now();

        // CommissionLedger는 Attribution이 직접 생성 (Aggregate 불변식)
        this.commissionLedger = new CommissionLedger(
            campaignId,
            creatorId,
            sellerId,
            commissionAmount
        );
    }

    /**
     * 커미션 확정 (구매 확정 시)
     * CommissionLedger의 상태를 CONFIRMED로 변경
     */
    public void confirmCommission() {
        this.commissionLedger.confirm();
    }

    /**
     * 커미션 지급 완료
     * CommissionLedger의 상태를 PAID로 변경
     */
    public void markCommissionAsPaid() {
        this.commissionLedger.markAsPaid();
    }

    /**
     * 커미션 취소 (환불 등)
     * CommissionLedger의 상태를 CANCELLED로 변경
     */
    public void cancelCommission() {
        this.commissionLedger.cancel();
    }

    /**
     * 재구성용 생성자 (infrastructure에서 DB 로드 시 사용)
     */
    public Attribution(Long id, Long orderId, Long clickId, Long campaignId,
                       LocalDateTime attributedAt, CommissionLedger commissionLedger) {
        this.id = id;
        this.orderId = orderId;
        this.clickId = clickId;
        this.campaignId = campaignId;
        this.attributedAt = attributedAt;
        this.commissionLedger = commissionLedger;
    }

    private void validateConstructorParams(Long orderId, Long clickId, Long campaignId,
                                           Long creatorId, Long sellerId, BigDecimal commissionAmount) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId는 필수입니다.");
        }
        if (clickId == null) {
            throw new IllegalArgumentException("clickId는 필수입니다.");
        }
        if (campaignId == null) {
            throw new IllegalArgumentException("campaignId는 필수입니다.");
        }
        if (creatorId == null) {
            throw new IllegalArgumentException("creatorId는 필수입니다.");
        }
        if (sellerId == null) {
            throw new IllegalArgumentException("sellerId는 필수입니다.");
        }
        if (commissionAmount == null || commissionAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("commissionAmount는 0보다 커야 합니다.");
        }
    }
}
