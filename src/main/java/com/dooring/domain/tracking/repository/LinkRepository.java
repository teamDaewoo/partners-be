package com.dooring.domain.tracking.repository;

import com.dooring.domain.tracking.entity.Link;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 어필리에이트 링크 Repository
 */
public interface LinkRepository extends JpaRepository<Link, Long> {

    /**
     * 단축 코드로 링크 조회
     *
     * @param shortCode 단축 코드 (e.g. "aB3kX9")
     * @return 링크
     */
    Optional<Link> findByShortCode(String shortCode);

    /**
     * 크리에이터 × 상품으로 링크 조회
     * 크리에이터당 상품별로 1개만 존재 (UNIQUE 제약)
     *
     * @param creatorId 크리에이터 ID
     * @param productId 상품 ID
     * @return 링크
     */
    Optional<Link> findByCreatorIdAndProductId(Long creatorId, Long productId);

    /**
     * 크리에이터 × 상품 링크 존재 여부 확인
     *
     * @param creatorId 크리에이터 ID
     * @param productId 상품 ID
     * @return 존재 여부
     */
    boolean existsByCreatorIdAndProductId(Long creatorId, Long productId);

    /**
     * 크리에이터의 모든 링크 조회
     *
     * @param creatorId 크리에이터 ID
     * @return 링크 목록
     */
    List<Link> findAllByCreatorId(Long creatorId);

    /**
     * 상품의 모든 링크 조회
     *
     * @param productId 상품 ID
     * @return 링크 목록
     */
    List<Link> findAllByProductId(Long productId);
}
