package com.dooring.domain.attribution.aggregate;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 커미션 정산 원장 (Attribution에 종속)
 * Attribution 없이 단독 존재 불가
 */
@Getter
public class CommissionLedger {

    private Long id;
    private final Long campaignId;
    private final Long creatorId;
    private final Long sellerId;
    private final BigDecimal amount;
    private CommissionStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime paidAt;

    /**
     * 커미션 원장 생성 (Attribution에서만 생성)
     */
    public CommissionLedger(Long campaignId, Long creatorId, Long sellerId, BigDecimal amount) {
        this.campaignId = campaignId;
        this.creatorId = creatorId;
        this.sellerId = sellerId;
        this.amount = amount;
        this.status = CommissionStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 커미션 확정 (구매 확정 시)
     * PENDING → CONFIRMED
     */
    public void confirm() {
        if (this.status != CommissionStatus.PENDING) {
            throw new IllegalStateException(
                "커미션 확정은 PENDING 상태에서만 가능합니다. 현재 상태: " + this.status
            );
        }
        this.status = CommissionStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 커미션 지급 완료
     * CONFIRMED → PAID
     */
    public void markAsPaid() {
        if (this.status != CommissionStatus.CONFIRMED) {
            throw new IllegalStateException(
                "커미션 지급은 CONFIRMED 상태에서만 가능합니다. 현재 상태: " + this.status
            );
        }
        this.status = CommissionStatus.PAID;
        this.paidAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 커미션 취소 (환불 등)
     * PENDING 또는 CONFIRMED → CANCELLED
     */
    public void cancel() {
        if (this.status == CommissionStatus.PAID) {
            throw new IllegalStateException(
                "이미 지급된 커미션은 취소할 수 없습니다."
            );
        }
        if (this.status == CommissionStatus.CANCELLED) {
            throw new IllegalStateException(
                "이미 취소된 커미션입니다."
            );
        }
        this.status = CommissionStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 재구성용 생성자 (infrastructure에서 DB 로드 시 사용)
     */
    public CommissionLedger(Long id, Long campaignId, Long creatorId, Long sellerId,
                            BigDecimal amount, CommissionStatus status,
                            LocalDateTime createdAt, LocalDateTime updatedAt,
                            LocalDateTime confirmedAt, LocalDateTime paidAt) {
        this.id = id;
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
}
