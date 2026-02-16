package com.dooring.infrastructure.persistence.attribution;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Attribution JPA Repository
 * PersistenceAdapter에서만 사용 (도메인에서 직접 사용 금지)
 */
public interface AttributionJpaRepository extends JpaRepository<AttributionJpaEntity, Long> {

    /**
     * 주문 ID로 조회 (UNIQUE)
     */
    Optional<AttributionJpaEntity> findByOrderId(Long orderId);

    /**
     * 주문 ID 존재 여부 확인 (멱등성 체크)
     */
    boolean existsByOrderId(Long orderId);

    /**
     * 클릭 ID로 조회
     */
    Optional<AttributionJpaEntity> findByClickId(Long clickId);

    /**
     * 캠페인별 조회
     */
    List<AttributionJpaEntity> findAllByCampaignId(Long campaignId);

    /**
     * 크리에이터별 조회 (CommissionLedger 조인)
     */
    @Query("SELECT a FROM AttributionJpaEntity a JOIN FETCH a.commissionLedger cl WHERE cl.creatorId = :creatorId")
    List<AttributionJpaEntity> findAllByCreatorId(@Param("creatorId") Long creatorId);

    /**
     * 셀러별 조회 (CommissionLedger 조인)
     */
    @Query("SELECT a FROM AttributionJpaEntity a JOIN FETCH a.commissionLedger cl WHERE cl.sellerId = :sellerId")
    List<AttributionJpaEntity> findAllBySellerId(@Param("sellerId") Long sellerId);
}
