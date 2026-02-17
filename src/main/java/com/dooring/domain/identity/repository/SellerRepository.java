package com.dooring.domain.identity.repository;

import com.dooring.domain.identity.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 셀러 Repository
 */
public interface SellerRepository extends JpaRepository<Seller, Long> {

    /**
     * 이메일로 셀러 조회
     *
     * @param email 이메일
     * @return 셀러
     */
    Optional<Seller> findByEmail(String email);

    /**
     * 이메일 존재 여부 확인
     *
     * @param email 이메일
     * @return 존재 여부
     */
    boolean existsByEmail(String email);
}
