package com.dooring.domain.attribution.aggregate;

/**
 * 커미션 상태
 */
public enum CommissionStatus {
    /**
     * 커미션 대기 (구매 확정 전)
     */
    PENDING,

    /**
     * 커미션 확정 (구매 확정 후, 셀러 지급 의무 발생)
     */
    CONFIRMED,

    /**
     * 셀러가 크리에이터에게 지급 완료
     */
    PAID,

    /**
     * 커미션 취소 (환불 등)
     */
    CANCELLED
}
