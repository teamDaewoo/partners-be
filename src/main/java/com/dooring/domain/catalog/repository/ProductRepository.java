package com.dooring.domain.catalog.repository;

import com.dooring.domain.catalog.entity.Product;
import com.dooring.domain.catalog.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 상품 Repository
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * 스토어의 모든 상품 조회
     *
     * @param store 스토어
     * @return 상품 목록
     */
    List<Product> findAllByStore(Store store);

    /**
     * 스토어의 활성 상품 조회
     *
     * @param store 스토어
     * @return 활성 상품 목록
     */
    List<Product> findAllByStoreAndIsActiveTrue(Store store);

    /**
     * 스토어와 외부 상품 ID로 조회
     *
     * @param store 스토어
     * @param externalProductId 외부 상품 ID
     * @return 상품
     */
    Optional<Product> findByStoreAndExternalProductId(Store store, String externalProductId);

    /**
     * 스토어와 외부 상품 ID 존재 여부 확인
     *
     * @param store 스토어
     * @param externalProductId 외부 상품 ID
     * @return 존재 여부
     */
    boolean existsByStoreAndExternalProductId(Store store, String externalProductId);

    /**
     * 스토어 ID로 모든 상품 조회
     *
     * @param storeId 스토어 ID
     * @return 상품 목록
     */
    @Query("SELECT p FROM Product p WHERE p.store.id = :storeId")
    List<Product> findAllByStoreId(@Param("storeId") Long storeId);

    /**
     * 스토어 ID로 활성 상품 조회
     *
     * @param storeId 스토어 ID
     * @return 활성 상품 목록
     */
    @Query("SELECT p FROM Product p WHERE p.store.id = :storeId AND p.isActive = true")
    List<Product> findAllActiveByStoreId(@Param("storeId") Long storeId);
}
