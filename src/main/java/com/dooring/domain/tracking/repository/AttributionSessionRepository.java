package com.dooring.domain.tracking.repository;

import com.dooring.domain.tracking.entity.AttributionSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 어트리뷰션 세션 Repository
 */
public interface AttributionSessionRepository extends JpaRepository<AttributionSession, Long> {

    /**
     * 세션 토큰으로 조회
     *
     * @param sessionToken 세션 토큰
     * @return 어트리뷰션 세션
     */
    Optional<AttributionSession> findBySessionToken(String sessionToken);

    /**
     * 클릭 ID로 모든 세션 조회
     *
     * @param clickId 클릭 ID
     * @return 세션 목록
     */
    @Query("SELECT s FROM AttributionSession s WHERE s.click.id = :clickId")
    List<AttributionSession> findAllByClickId(@Param("clickId") Long clickId);

    /**
     * 만료되지 않은 세션만 조회
     *
     * @param sessionToken 세션 토큰
     * @param now 현재 시각
     * @return 어트리뷰션 세션
     */
    @Query("SELECT s FROM AttributionSession s WHERE s.sessionToken = :sessionToken AND s.expiresAt > :now")
    Optional<AttributionSession> findValidSessionByToken(@Param("sessionToken") String sessionToken,
                                                          @Param("now") LocalDateTime now);

    /**
     * 만료된 세션 삭제 (배치 정리용)
     *
     * @param expiresAt 만료 시각 기준
     * @return 삭제된 세션 수
     */
    @Modifying
    @Query("DELETE FROM AttributionSession s WHERE s.expiresAt < :expiresAt")
    int deleteByExpiresAtBefore(@Param("expiresAt") LocalDateTime expiresAt);

    /**
     * 만료된 세션 개수 조회
     *
     * @param now 현재 시각
     * @return 만료된 세션 수
     */
    long countByExpiresAtBefore(LocalDateTime now);
}
