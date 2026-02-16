package com.dooring.domain.catalog.repository;

import com.dooring.domain.catalog.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 스토어 Repository
 */
public interface StoreRepository extends JpaRepository<Store, Long> {

    /**
     * 셀러의 모든 스토어 조회
     *
     * @param sellerId 셀러 ID
     * @return 스토어 목록
     */
    List<Store> findAllBySellerId(Long sellerId);

    /**
     * 셀러의 활성 스토어 조회
     *
     * @param sellerId 셀러 ID
     * @return 활성 스토어 목록
     */
    List<Store> findAllBySellerIdAndIsActiveTrue(Long sellerId);

    /**
     * 플랫폼과 외부 스토어 ID로 조회
     *
     * @param platformId 플랫폼 ID
     * @param externalStoreId 외부 스토어 ID
     * @return 스토어
     */
    Optional<Store> findByPlatformIdAndExternalStoreId(Long platformId, String externalStoreId);

    /**
     * 플랫폼과 외부 스토어 ID 존재 여부 확인
     *
     * @param platformId 플랫폼 ID
     * @param externalStoreId 외부 스토어 ID
     * @return 존재 여부
     */
    boolean existsByPlatformIdAndExternalStoreId(Long platformId, String externalStoreId);

    /**
     * 플랫폼의 모든 스토어 조회
     *
     * @param platformId 플랫폼 ID
     * @return 스토어 목록
     */
    List<Store> findAllByPlatformId(Long platformId);
}
