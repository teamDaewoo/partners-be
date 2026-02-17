package com.dooring.domain.identity.repository;

import com.dooring.domain.identity.entity.Platform;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 플랫폼 Repository
 */
public interface PlatformRepository extends JpaRepository<Platform, Long> {

    /**
     * 플랫폼 코드로 조회
     *
     * @param code 플랫폼 코드 (e.g. "cafe24", "imweb")
     * @return 플랫폼
     */
    Optional<Platform> findByCode(String code);

    /**
     * 플랫폼 코드 존재 여부 확인
     *
     * @param code 플랫폼 코드
     * @return 존재 여부
     */
    boolean existsByCode(String code);
}
