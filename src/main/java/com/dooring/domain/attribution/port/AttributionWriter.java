package com.dooring.domain.attribution.port;

import com.dooring.domain.attribution.aggregate.Attribution;

/**
 * Attribution 저장 Port
 * 구현체는 infrastructure/persistence/attribution/에 위치
 */
public interface AttributionWriter {

    /**
     * Attribution 저장 (CommissionLedger도 함께 원자적으로 저장)
     *
     * 멱등성: 같은 orderId에 대해 중복 저장 시도 시 예외 발생
     * 원자성: Attribution과 CommissionLedger를 하나의 트랜잭션으로 저장
     *
     * @param attribution 귀속 Aggregate
     * @throws IllegalStateException 이미 존재하는 orderId일 경우
     */
    void save(Attribution attribution);

    /**
     * Attribution 저장 (멱등 처리 - 중복 시 무시)
     *
     * 이미 존재하는 orderId면 저장하지 않고 기존 것을 반환
     * 동시성 환경에서 안전한 멱등 저장
     *
     * @param attribution 귀속 Aggregate
     * @return 저장되거나 이미 존재하는 Attribution
     */
    Attribution saveIdempotent(Attribution attribution);
}
