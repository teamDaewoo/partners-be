package com.dooring.domain.order.entity;

/**
 * 주문 상태
 */
public enum OrderStatus {
    /**
     * 주문 생성
     */
    CREATED,

    /**
     * 결제 완료
     */
    PAID,

    /**
     * 배송 완료
     */
    DELIVERED,

    /**
     * 구매 확정
     */
    CONFIRMED,

    /**
     * 주문 취소
     */
    CANCELLED,

    /**
     * 환불 완료
     */
    REFUNDED
}
