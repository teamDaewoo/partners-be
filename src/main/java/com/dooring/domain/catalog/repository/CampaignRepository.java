package com.dooring.domain.catalog.repository;

import com.dooring.domain.catalog.entity.Campaign;
import com.dooring.domain.catalog.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 캠페인 Repository
 */
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    /**
     * 상품의 활성 캠페인 조회 (상품당 1개만 존재)
     *
     * @param product 상품
     * @return 활성 캠페인
     */
    Optional<Campaign> findByProductAndIsActiveTrue(Product product);

    /**
     * 상품 ID로 활성 캠페인 조회
     *
     * @param productId 상품 ID
     * @return 활성 캠페인
     */
    @Query("SELECT c FROM Campaign c WHERE c.product.id = :productId AND c.isActive = true")
    Optional<Campaign> findActiveByProductId(@Param("productId") Long productId);

    /**
     * 셀러의 모든 캠페인 조회
     *
     * @param sellerId 셀러 ID
     * @return 캠페인 목록
     */
    List<Campaign> findAllBySellerId(Long sellerId);

    /**
     * 셀러의 활성 캠페인 조회
     *
     * @param sellerId 셀러 ID
     * @return 활성 캠페인 목록
     */
    List<Campaign> findAllBySellerIdAndIsActiveTrue(Long sellerId);

    /**
     * 상품의 모든 캠페인 조회
     *
     * @param product 상품
     * @return 캠페인 목록
     */
    List<Campaign> findAllByProduct(Product product);

    /**
     * 상품 ID로 모든 캠페인 조회
     *
     * @param productId 상품 ID
     * @return 캠페인 목록
     */
    @Query("SELECT c FROM Campaign c WHERE c.product.id = :productId")
    List<Campaign> findAllByProductId(@Param("productId") Long productId);

    /**
     * 특정 기간 내 활성 캠페인 조회
     *
     * @param startTime 시작 시각
     * @param endTime 종료 시각
     * @return 캠페인 목록
     */
    @Query("SELECT c FROM Campaign c WHERE c.isActive = true " +
           "AND c.startsAt <= :endTime AND c.endsAt >= :startTime")
    List<Campaign> findActiveCampaignsInPeriod(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 상품의 활성 캠페인 존재 여부 확인
     *
     * @param product 상품
     * @return 존재 여부
     */
    boolean existsByProductAndIsActiveTrue(Product product);
}
