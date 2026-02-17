package com.dooring.domain.identity.repository;

import com.dooring.domain.identity.entity.Creator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 크리에이터 Repository
 */
public interface CreatorRepository extends JpaRepository<Creator, Long> {

    /**
     * 이메일로 크리에이터 조회
     *
     * @param email 이메일
     * @return 크리에이터
     */
    Optional<Creator> findByEmail(String email);

    /**
     * 닉네임으로 크리에이터 조회
     *
     * @param nickname 닉네임
     * @return 크리에이터
     */
    Optional<Creator> findByNickname(String nickname);

    /**
     * 이메일 존재 여부 확인
     *
     * @param email 이메일
     * @return 존재 여부
     */
    boolean existsByEmail(String email);

    /**
     * 닉네임 존재 여부 확인
     *
     * @param nickname 닉네임
     * @return 존재 여부
     */
    boolean existsByNickname(String nickname);
}
