package com.dooring.domain.attribution.port;

import com.dooring.domain.attribution.aggregate.Attribution;

import java.util.List;
import java.util.Optional;

/**
 * Attribution 조회 Port
 * 구현체는 infrastructure/persistence/attribution/에 위치
 */
public interface AttributionReader {

    /**
     * ID로 Attribution 조회
     *
     * @param id Attribution ID
     * @return Attribution (CommissionLedger 포함)
     */
    Optional<Attribution> findById(Long id);

    /**
     * 주문 ID로 Attribution 조회
     *
     * @param orderId 주문 ID
     * @return Attribution (CommissionLedger 포함)
     */
    Optional<Attribution> findByOrderId(Long orderId);

    /**
     * 주문 ID로 Attribution 존재 여부 확인
     * 멱등성 체크용 - 이미 귀속이 생성되었는지 확인
     *
     * @param orderId 주문 ID
     * @return 존재 여부
     */
    boolean existsByOrderId(Long orderId);

    /**
     * 클릭 ID로 Attribution 조회
     *
     * @param clickId 클릭 ID
     * @return Attribution (CommissionLedger 포함)
     */
    Optional<Attribution> findByClickId(Long clickId);

    /**
     * 캠페인별 모든 Attribution 조회
     *
     * @param campaignId 캠페인 ID
     * @return Attribution 목록
     */
    List<Attribution> findAllByCampaignId(Long campaignId);

    /**
     * 크리에이터별 모든 Attribution 조회
     * CommissionLedger의 creatorId 기준
     *
     * @param creatorId 크리에이터 ID
     * @return Attribution 목록
     */
    List<Attribution> findAllByCreatorId(Long creatorId);

    /**
     * 셀러별 모든 Attribution 조회
     * CommissionLedger의 sellerId 기준
     *
     * @param sellerId 셀러 ID
     * @return Attribution 목록
     */
    List<Attribution> findAllBySellerId(Long sellerId);
}
